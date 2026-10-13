package com.smartmall.tlai.agent.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmall.tlai.agent.model.AgentChatResult;
import com.smartmall.tlai.agent.model.AgentConversation;
import com.smartmall.tlai.agent.model.AgentConversationMessage;
import com.smartmall.tlai.agent.model.AgentConversationState;
import com.smartmall.tlai.agent.model.AgentConversationStatus;
import com.smartmall.tlai.agent.model.AgentConversationTurn;
import com.smartmall.tlai.agent.model.AgentUserContext;
import com.smartmall.tlai.agent.repository.AgentConversationRepository;
import com.smartmall.tlai.ai.model.AiChatMessage;
import com.smartmall.tlai.ai.model.AiCallContext;
import com.smartmall.tlai.ai.model.AiMessageRole;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class AgentConversationService {
    private static final int CONTEXT_MESSAGE_LIMIT = 30;
    private static final int HISTORY_MESSAGE_LIMIT = 200;
    private static final int MAX_MESSAGE_LENGTH = 4000;

    private final AgentConversationRepository repository;
    private final ReadOnlyAgentService agentService;
    private final ObjectMapper objectMapper;

    public AgentConversationService(AgentConversationRepository repository,
                                    ReadOnlyAgentService agentService,
                                    ObjectMapper objectMapper) {
        this.repository = repository;
        this.agentService = agentService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AgentConversation create(AgentUserContext user) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return repository.insertConversation(new AgentConversation(null, user.userId(), "新对话",
                AgentConversationStatus.ACTIVE, null, now, now, now));
    }

    @Transactional(readOnly = true)
    public PageResponse<AgentConversation> list(AgentUserContext user, int page, int size) {
        return new PageResponse<>(repository.findOwnedConversations(user.userId(), page, size),
                repository.countOwnedConversations(user.userId()), page, size);
    }

    @Transactional(readOnly = true)
    public List<AgentConversationMessage> messages(long conversationId, AgentUserContext user) {
        requireOwned(conversationId, user, false);
        return repository.findMessages(conversationId, user.userId(), HISTORY_MESSAGE_LIMIT);
    }

    @Transactional
    public void archive(long conversationId, AgentUserContext user) {
        requireOwned(conversationId, user, false);
        if (!repository.archiveConversation(conversationId, user.userId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该会话已经归档");
        }
    }

    public AgentConversationTurn reply(long conversationId, String content, AgentUserContext user) {
        String normalized = normalizeMessage(content);
        AgentConversation conversation = requireOwned(conversationId, user, true);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        AgentConversationMessage userMessage = repository.insertMessage(new AgentConversationMessage(
                null, conversationId, user.userId(), AiMessageRole.USER, normalized,
                null, null, null, List.of(), List.of(), List.of(), now));

        String title = "新对话".equals(conversation.title()) ? createTitle(normalized) : conversation.title();
        repository.updateConversation(conversationId, user.userId(), title, conversation.stateJson(), now);
        List<AiChatMessage> history = repository.findRecentMessages(
                        conversationId, user.userId(), CONTEXT_MESSAGE_LIMIT).stream()
                .map(message -> new AiChatMessage(message.role(), message.content()))
                .toList();

        AgentConversationState state = parseState(conversation.stateJson());
        AiCallContext callContext = new AiCallContext(user.userId(), UUID.randomUUID().toString(), conversationId);
        AgentChatResult reply = agentService.chat(history, user, state, callContext);
        OffsetDateTime completedAt = OffsetDateTime.now(ZoneOffset.UTC);
        AgentConversationMessage assistantMessage = repository.insertMessage(new AgentConversationMessage(
                null, conversationId, user.userId(), AiMessageRole.ASSISTANT, reply.content(),
                reply.responseType(), reply.modelName(), reply.latencyMs(), reply.tools(),
                reply.suggestions(), reply.missingFields(), completedAt));
        String stateJson = reply.conversationState() == null ? null : toJson(reply.conversationState());
        repository.updateConversation(conversationId, user.userId(), title, stateJson, completedAt);
        AgentConversation updated = repository.findOwnedConversation(conversationId, user.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "AI 会话不存在"));
        return new AgentConversationTurn(updated, userMessage, assistantMessage, reply);
    }

    private AgentConversation requireOwned(long conversationId, AgentUserContext user, boolean activeRequired) {
        AgentConversation conversation = repository.findOwnedConversation(conversationId, user.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "AI 会话不存在"));
        if (activeRequired && conversation.status() != AgentConversationStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已归档会话不能继续发送消息");
        }
        return conversation;
    }

    private AgentConversationState parseState(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, AgentConversationState.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 会话状态无法读取");
        }
    }

    private String toJson(AgentConversationState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 会话状态无法保存");
        }
    }

    private static String normalizeMessage(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_MESSAGE_LENGTH) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息长度必须在 1 到 4000 个字符之间");
        }
        return normalized;
    }

    private static String createTitle(String content) {
        String singleLine = content.replaceAll("\\s+", " ");
        return singleLine.length() <= 36 ? singleLine : singleLine.substring(0, 36) + "...";
    }
}

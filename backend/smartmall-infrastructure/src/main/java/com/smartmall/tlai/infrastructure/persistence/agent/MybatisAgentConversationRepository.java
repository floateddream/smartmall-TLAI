package com.smartmall.tlai.infrastructure.persistence.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmall.tlai.agent.model.AgentConversation;
import com.smartmall.tlai.agent.model.AgentConversationMessage;
import com.smartmall.tlai.agent.model.AgentConversationStatus;
import com.smartmall.tlai.agent.model.AgentResponseType;
import com.smartmall.tlai.agent.model.AgentSuggestion;
import com.smartmall.tlai.agent.model.AgentToolExecution;
import com.smartmall.tlai.agent.repository.AgentConversationRepository;
import com.smartmall.tlai.ai.model.AiMessageRole;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MybatisAgentConversationRepository implements AgentConversationRepository {
    private static final TypeReference<List<AgentToolExecution>> TOOL_LIST = new TypeReference<>() { };
    private static final TypeReference<List<AgentSuggestion>> SUGGESTION_LIST = new TypeReference<>() { };
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

    private final AgentConversationMapper conversationMapper;
    private final AgentConversationMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    public MybatisAgentConversationRepository(AgentConversationMapper conversationMapper,
                                              AgentConversationMessageMapper messageMapper,
                                              ObjectMapper objectMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentConversation insertConversation(AgentConversation conversation) {
        AgentConversationPersistenceEntity entity = toEntity(conversation);
        conversationMapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<AgentConversation> findOwnedConversation(long id, long userId) {
        return Optional.ofNullable(conversationMapper.selectOwned(id, userId)).map(this::toDomain);
    }

    @Override
    public List<AgentConversation> findOwnedConversations(long userId, int page, int size) {
        long offset = ((long) page - 1) * size;
        return conversationMapper.selectOwnedPage(userId, offset, size).stream().map(this::toDomain).toList();
    }

    @Override
    public long countOwnedConversations(long userId) {
        return conversationMapper.countOwned(userId);
    }

    @Override
    public void updateConversation(long id, long userId, String title, String stateJson,
                                   OffsetDateTime lastMessageAt) {
        conversationMapper.updateConversation(id, userId, title, stateJson, lastMessageAt);
    }

    @Override
    public boolean archiveConversation(long id, long userId) {
        return conversationMapper.archive(id, userId) == 1;
    }

    @Override
    public AgentConversationMessage insertMessage(AgentConversationMessage message) {
        AgentConversationMessagePersistenceEntity entity = toEntity(message);
        messageMapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public List<AgentConversationMessage> findRecentMessages(long conversationId, long userId, int limit) {
        return messageMapper.selectRecent(conversationId, userId, limit).stream().map(this::toDomain).toList();
    }

    @Override
    public List<AgentConversationMessage> findMessages(long conversationId, long userId, int limit) {
        return findRecentMessages(conversationId, userId, limit);
    }

    private AgentConversation toDomain(AgentConversationPersistenceEntity entity) {
        return new AgentConversation(entity.getId(), entity.getUserId(), entity.getTitle(),
                AgentConversationStatus.valueOf(entity.getStatus()), entity.getStateJson(),
                entity.getLastMessageAt(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private AgentConversationPersistenceEntity toEntity(AgentConversation conversation) {
        AgentConversationPersistenceEntity entity = new AgentConversationPersistenceEntity();
        entity.setId(conversation.id());
        entity.setUserId(conversation.userId());
        entity.setTitle(conversation.title());
        entity.setStatus(conversation.status().name());
        entity.setStateJson(conversation.stateJson());
        entity.setLastMessageAt(conversation.lastMessageAt());
        entity.setCreatedAt(conversation.createdAt());
        entity.setUpdatedAt(conversation.updatedAt());
        return entity;
    }

    private AgentConversationMessage toDomain(AgentConversationMessagePersistenceEntity entity) {
        return new AgentConversationMessage(entity.getId(), entity.getConversationId(), entity.getUserId(),
                AiMessageRole.valueOf(entity.getRole()), entity.getContent(),
                entity.getResponseType() == null ? null : AgentResponseType.valueOf(entity.getResponseType()),
                entity.getModelName(), entity.getLatencyMs(),
                read(entity.getToolsJson(), TOOL_LIST), read(entity.getSuggestionsJson(), SUGGESTION_LIST),
                read(entity.getMissingFieldsJson(), STRING_LIST), entity.getCreatedAt());
    }

    private AgentConversationMessagePersistenceEntity toEntity(AgentConversationMessage message) {
        AgentConversationMessagePersistenceEntity entity = new AgentConversationMessagePersistenceEntity();
        entity.setId(message.id());
        entity.setConversationId(message.conversationId());
        entity.setUserId(message.userId());
        entity.setRole(message.role().name());
        entity.setContent(message.content());
        entity.setResponseType(message.responseType() == null ? null : message.responseType().name());
        entity.setModelName(message.modelName());
        entity.setLatencyMs(message.latencyMs());
        entity.setToolsJson(write(message.tools()));
        entity.setSuggestionsJson(write(message.suggestions()));
        entity.setMissingFieldsJson(write(message.missingFields()));
        entity.setCreatedAt(message.createdAt());
        return entity;
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 会话消息无法序列化");
        }
    }

    private <T> T read(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json == null ? "[]" : json, type);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 会话消息无法读取");
        }
    }
}

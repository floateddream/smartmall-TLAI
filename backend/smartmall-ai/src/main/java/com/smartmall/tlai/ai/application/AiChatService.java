package com.smartmall.tlai.ai.application;

import com.smartmall.tlai.ai.config.AiProperties;
import com.smartmall.tlai.ai.gateway.AiChatGateway;
import com.smartmall.tlai.ai.model.AiChatMessage;
import com.smartmall.tlai.ai.model.AiChatResult;
import com.smartmall.tlai.ai.model.AiCallScene;
import com.smartmall.tlai.ai.model.AiCallStatus;
import com.smartmall.tlai.ai.model.AiCallContext;
import com.smartmall.tlai.ai.model.AiMessageRole;
import com.smartmall.tlai.ai.model.AiModelConfig;
import com.smartmall.tlai.ai.model.AiModelType;
import com.smartmall.tlai.ai.repository.AiModelConfigRepository;
import com.smartmall.tlai.ai.security.AiCredentialCipher;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AiChatService {
    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);
    private static final int MAX_HISTORY_MESSAGES = 30;
    private static final int MAX_MESSAGE_LENGTH = 4000;
    private static final String SYSTEM_PROMPT = """
            你是 SmartMall TLAI 的电商运营助手。回答应准确、简洁，并明确区分事实与建议。
            当前基础阶段尚未开放业务工具调用，因此不要声称已经查询数据库、修改订单或执行退款。
            用户要求执行系统操作时，应说明当前只能提供分析建议。
            """;

    private final AiModelConfigRepository repository;
    private final AiChatGateway gateway;
    private final AiProperties properties;
    private final AiCredentialCipher credentialCipher;
    private final AiCallLogService callLogService;

    public AiChatService(AiModelConfigRepository repository, AiChatGateway gateway,
                         AiProperties properties, AiCredentialCipher credentialCipher,
                         AiCallLogService callLogService) {
        this.repository = repository;
        this.gateway = gateway;
        this.properties = properties;
        this.credentialCipher = credentialCipher;
        this.callLogService = callLogService;
    }

    @Transactional(readOnly = true)
    public AiChatResult chat(List<AiChatMessage> requestedMessages, AiCallContext context) {
        return complete(validateMessages(requestedMessages), SYSTEM_PROMPT,
                AiCallScene.ASSISTANT_CHAT, context);
    }

    /** 供 RAG 等内部用例追加可信系统上下文，普通用户消息不能直接控制该内容。 */
    @Transactional(readOnly = true)
    public AiChatResult chatWithContext(List<AiChatMessage> requestedMessages, String systemContext,
                                        AiCallContext context) {
        List<AiChatMessage> messages = validateMessages(requestedMessages);
        return complete(messages, buildSystemPrompt(systemContext), AiCallScene.KNOWLEDGE_RAG, context);
    }

    /** 供 BI 等内部编排使用固定系统提示词，提示词不从 HTTP 请求直接接收。 */
    @Transactional(readOnly = true)
    public AiChatResult chatWithSystemPrompt(List<AiChatMessage> requestedMessages, String systemPrompt,
                                             AiCallContext context) {
        if (systemPrompt == null || systemPrompt.isBlank() || systemPrompt.length() > 20_000) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "AI 系统提示词不能为空且不能超过 20000 个字符");
        }
        return complete(validateMessages(requestedMessages), systemPrompt.trim(),
                AiCallScene.INTERNAL_ORCHESTRATION, context);
    }

    private AiChatResult complete(List<AiChatMessage> messages, String systemPrompt, AiCallScene scene,
                                  AiCallContext context) {
        if (context == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 调用缺少用户上下文");
        }
        AiModelConfig config = repository.findActiveForOwner(AiModelType.CHAT, context.userId())
                .filter(AiModelConfig::enabled)
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_USER_MODEL_NOT_CONFIGURED));
        log.info("AI 对话开始：用户ID={}，配置ID={}，供应商={}，模型={}，消息数量={}",
                context.userId(), config.id(), config.provider(), config.modelName(), messages.size());
        long startedAt = System.nanoTime();
        AiChatResult result;
        try {
            String apiKey = config.credentialRequired()
                    ? credentialCipher.decrypt(config.apiKeyCiphertext()) : "ollama";
            result = gateway.complete(config, messages, systemPrompt,
                    apiKey, properties.requestTimeout());
        } catch (RuntimeException exception) {
            recordSafely(config, scene, AiCallStatus.FAILED, elapsedMs(startedAt), messages.size(),
                    inputCharacters(messages, systemPrompt), 0, null,
                    AiCallLogService.errorCode(exception), context);
            throw exception;
        }
        recordSafely(config, scene, AiCallStatus.SUCCESS, elapsedMs(startedAt), messages.size(),
                inputCharacters(messages, systemPrompt), 1, (long) result.content().length(), null, context);
        log.info("AI 对话成功：配置ID={}，模型={}，耗时毫秒={}",
                config.id(), result.modelName(), result.latencyMs());
        return result;
    }

    private void recordSafely(AiModelConfig config, AiCallScene scene, AiCallStatus status, long latencyMs,
                              int inputCount, long inputCharacterCount, int outputCount,
                              Long outputUnitCount, String errorCode, AiCallContext context) {
        try {
            callLogService.record(config, scene, status, latencyMs, inputCount, inputCharacterCount,
                    outputCount, outputUnitCount, errorCode, context);
        } catch (RuntimeException exception) {
            log.error("AI 调用日志保存失败：配置ID={}，类型={}，调用场景={}，状态={}",
                    config.id(), config.modelType(), scene, status, exception);
        }
    }

    private static long inputCharacters(List<AiChatMessage> messages, String systemPrompt) {
        return systemPrompt.length() + messages.stream().mapToLong(message -> message.content().length()).sum();
    }

    private static long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private static String buildSystemPrompt(String systemContext) {
        if (systemContext == null || systemContext.isBlank()) return SYSTEM_PROMPT;
        if (systemContext.length() > 16_000) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "AI 系统上下文不能超过 16000 个字符");
        }
        return SYSTEM_PROMPT + "\n\n" + systemContext.trim();
    }

    private static List<AiChatMessage> validateMessages(List<AiChatMessage> requestedMessages) {
        if (requestedMessages == null || requestedMessages.isEmpty()
                || requestedMessages.size() > MAX_HISTORY_MESSAGES) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "对话消息数量必须在 1 到 30 条之间");
        }
        List<AiChatMessage> messages = requestedMessages.stream().map(message -> {
            if (message == null || message.role() == null || message.content() == null
                    || message.content().isBlank() || message.content().length() > MAX_MESSAGE_LENGTH) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "消息内容不能为空且不能超过 4000 个字符");
            }
            return new AiChatMessage(message.role(), message.content().trim());
        }).toList();
        if (messages.getLast().role() != AiMessageRole.USER) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "最后一条消息必须来自用户");
        }
        return messages;
    }
}

package com.smartmall.tlai.ai.application;

import com.smartmall.tlai.ai.config.AiProperties;
import com.smartmall.tlai.ai.gateway.AiChatGateway;
import com.smartmall.tlai.ai.model.AiChatMessage;
import com.smartmall.tlai.ai.model.AiChatResult;
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

    public AiChatService(AiModelConfigRepository repository, AiChatGateway gateway,
                         AiProperties properties, AiCredentialCipher credentialCipher) {
        this.repository = repository;
        this.gateway = gateway;
        this.properties = properties;
        this.credentialCipher = credentialCipher;
    }

    @Transactional(readOnly = true)
    public AiChatResult chat(List<AiChatMessage> requestedMessages) {
        List<AiChatMessage> messages = validateMessages(requestedMessages);
        AiModelConfig config = repository.findActive(AiModelType.CHAT)
                .filter(AiModelConfig::enabled)
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_MODEL_NOT_CONFIGURED));
        log.info("AI chat started: configId={}, provider={}, model={}, messageCount={}",
                config.id(), config.provider(), config.modelName(), messages.size());
        AiChatResult result = gateway.complete(config, messages, SYSTEM_PROMPT,
                credentialCipher.decrypt(config.apiKeyCiphertext()), properties.requestTimeout());
        log.info("AI chat succeeded: configId={}, model={}, latencyMs={}",
                config.id(), result.modelName(), result.latencyMs());
        return result;
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

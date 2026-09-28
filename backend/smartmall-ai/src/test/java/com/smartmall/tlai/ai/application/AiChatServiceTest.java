package com.smartmall.tlai.ai.application;

import com.smartmall.tlai.ai.config.AiProperties;
import com.smartmall.tlai.ai.gateway.AiChatGateway;
import com.smartmall.tlai.ai.model.AiChatMessage;
import com.smartmall.tlai.ai.model.AiChatResult;
import com.smartmall.tlai.ai.model.AiMessageRole;
import com.smartmall.tlai.ai.model.AiModelConfig;
import com.smartmall.tlai.ai.model.AiModelType;
import com.smartmall.tlai.ai.model.AiProvider;
import com.smartmall.tlai.ai.repository.AiModelConfigRepository;
import com.smartmall.tlai.ai.security.AiCredentialCipher;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {
    @Mock AiModelConfigRepository repository;
    @Mock AiChatGateway gateway;
    private AiChatService service;
    private AiCredentialCipher credentialCipher;

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties(
                Base64.getEncoder().encodeToString(new byte[32]), Duration.ofSeconds(10));
        credentialCipher = new AiCredentialCipher(properties);
        service = new AiChatService(repository, gateway, properties, credentialCipher);
    }

    @Test
    void shouldUseActiveModelForChat() {
        AiModelConfig config = model(true, true);
        when(repository.findActive(AiModelType.CHAT)).thenReturn(Optional.of(config));
        when(gateway.complete(any(), anyList(), anyString(), anyString(), any()))
                .thenReturn(new AiChatResult("处理建议", "test-model", 120));

        AiChatResult result = service.chat(List.of(new AiChatMessage(AiMessageRole.USER, "分析退款问题")));

        assertThat(result.content()).isEqualTo("处理建议");
        assertThat(result.modelName()).isEqualTo("test-model");
    }

    @Test
    void shouldRejectChatWithoutActiveModel() {
        when(repository.findActive(AiModelType.CHAT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.chat(List.of(new AiChatMessage(AiMessageRole.USER, "你好"))))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.AI_MODEL_NOT_CONFIGURED));
    }

    @Test
    void shouldRequireLastMessageFromUser() {
        assertThatThrownBy(() -> service.chat(List.of(new AiChatMessage(AiMessageRole.ASSISTANT, "上一条回复"))))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    private AiModelConfig model(boolean active, boolean enabled) {
        OffsetDateTime now = OffsetDateTime.parse("2030-01-01T00:00:00Z");
        return new AiModelConfig(1L, "测试模型", AiModelType.CHAT,
                AiProvider.OPENAI_COMPATIBLE, "https://example.com/v1",
                "test-model", credentialCipher.encrypt("test-key"), BigDecimal.valueOf(0.7), 1024,
                null, active, enabled, 1L, 1L, now, now);
    }
}

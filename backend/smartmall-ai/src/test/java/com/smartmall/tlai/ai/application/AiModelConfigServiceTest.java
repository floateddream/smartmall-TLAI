package com.smartmall.tlai.ai.application;

import com.smartmall.tlai.ai.config.AiProperties;
import com.smartmall.tlai.ai.gateway.AiChatGateway;
import com.smartmall.tlai.ai.gateway.AiEmbeddingGateway;
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
import java.util.Optional;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AiModelConfigServiceTest {
    @Mock AiModelConfigRepository repository;
    @Mock AiChatGateway gateway;
    @Mock AiEmbeddingGateway embeddingGateway;
    @Mock AiCallLogService callLogService;
    private AiModelConfigService service;
    private AiCredentialCipher credentialCipher;

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties(
                Base64.getEncoder().encodeToString(new byte[32]), Duration.ofSeconds(10));
        credentialCipher = new AiCredentialCipher(properties);
        service = new AiModelConfigService(repository, gateway, embeddingGateway, properties, credentialCipher,
                callLogService);
    }

    @Test
    void shouldActivateEnabledModelExclusively() {
        AiModelConfig config = model(true, false);
        AiModelConfig activated = model(true, true);
        when(repository.findById(1L)).thenReturn(Optional.of(config), Optional.of(activated));

        AiModelConfig result = service.activate(1L, 9L);

        assertThat(result.active()).isTrue();
        verify(repository).lockConfiguration();
        verify(repository).clearActive(org.mockito.ArgumentMatchers.eq(AiModelType.CHAT),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(9L));
        verify(repository).updateActive(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(9L));
    }

    @Test
    void shouldRejectInvalidBaseUrl() {
        var command = new AiModelCommand("模型", AiModelType.CHAT, AiProvider.OPENAI_COMPATIBLE,
                "file:///tmp/model", "test", "test-key", BigDecimal.ONE, 1024, null, 1L);

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    void shouldRequireNewCredentialWhenProviderChanges() {
        when(repository.findById(1L)).thenReturn(Optional.of(model(true, false)));
        when(repository.findByName("测试模型")).thenReturn(Optional.empty());
        var command = new AiModelCommand("测试模型", AiModelType.CHAT, AiProvider.DEEPSEEK,
                "https://api.deepseek.com/v1", "deepseek-chat", "",
                BigDecimal.valueOf(0.7), 1024, null, 1L);

        assertThatThrownBy(() -> service.update(1L, command))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.AI_CREDENTIAL_MISSING));
    }

    @Test
    void shouldCreateOllamaEmbeddingWithoutApiKey() {
        when(repository.findByName("本地向量模型")).thenReturn(Optional.empty());
        when(repository.insert(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var command = new AiModelCommand("本地向量模型", AiModelType.EMBEDDING, AiProvider.OLLAMA,
                "http://localhost:11434/v1", "bge-m3", "", BigDecimal.valueOf(0.7), 2048, 1024, 1L);

        AiModelConfig created = service.create(command);

        assertThat(created.modelType()).isEqualTo(AiModelType.EMBEDDING);
        assertThat(created.embeddingDimensions()).isEqualTo(1024);
        assertThat(created.credentialConfigured()).isFalse();
    }

    @Test
    void shouldTestEmbeddingDimensions() {
        AiModelConfig config = embeddingModel();
        when(repository.findById(2L)).thenReturn(Optional.of(config));
        when(embeddingGateway.embed(any(), any(), any(), any())).thenReturn(new float[1024]);

        String result = service.test(2L);

        assertThat(result).contains("1024 维向量");
    }

    private AiModelConfig model(boolean enabled, boolean active) {
        OffsetDateTime now = OffsetDateTime.parse("2030-01-01T00:00:00Z");
        return new AiModelConfig(1L, "测试模型", AiModelType.CHAT,
                AiProvider.OPENAI_COMPATIBLE, "https://example.com/v1",
                "test-model", credentialCipher.encrypt("test-key"), BigDecimal.valueOf(0.7), 1024,
                null, active, enabled, 1L, 1L, now, now);
    }

    private AiModelConfig embeddingModel() {
        OffsetDateTime now = OffsetDateTime.parse("2030-01-01T00:00:00Z");
        return new AiModelConfig(2L, "本地向量模型", AiModelType.EMBEDDING, AiProvider.OLLAMA,
                "http://localhost:11434/v1", "bge-m3", null, BigDecimal.valueOf(0.7), 2048,
                1024, false, true, 1L, 1L, now, now);
    }
}

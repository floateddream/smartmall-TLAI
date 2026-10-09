package com.smartmall.tlai.ai.application;

import com.smartmall.tlai.ai.config.AiProperties;
import com.smartmall.tlai.ai.gateway.AiEmbeddingGateway;
import com.smartmall.tlai.ai.model.AiModelConfig;
import com.smartmall.tlai.ai.model.AiModelType;
import com.smartmall.tlai.ai.model.AiProvider;
import com.smartmall.tlai.ai.repository.AiModelConfigRepository;
import com.smartmall.tlai.ai.security.AiCredentialCipher;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiEmbeddingServiceTest {
    @Mock AiModelConfigRepository repository;
    @Mock AiEmbeddingGateway gateway;
    @Mock AiCredentialCipher credentialCipher;

    @Test
    void embedsAllInputsWithOneActiveConfiguration() {
        AiModelConfig config = embeddingConfig(3);
        when(repository.findActive(AiModelType.EMBEDDING)).thenReturn(Optional.of(config));
        when(gateway.embed(config, "第一段", "ollama", Duration.ofSeconds(60)))
                .thenReturn(new float[]{1, 2, 3});
        when(gateway.embed(config, "第二段", "ollama", Duration.ofSeconds(60)))
                .thenReturn(new float[]{4, 5, 6});
        var service = new AiEmbeddingService(repository, gateway,
                new AiProperties("", Duration.ofSeconds(60)), credentialCipher);

        List<float[]> result = service.embedAll(List.of("第一段", "第二段"));

        assertThat(result).hasSize(2);
        assertThat(result.getFirst()).containsExactly(1, 2, 3);
        verify(repository).findActive(AiModelType.EMBEDDING);
    }

    @Test
    void rejectsUnexpectedVectorDimensions() {
        AiModelConfig config = embeddingConfig(3);
        when(repository.findActive(AiModelType.EMBEDDING)).thenReturn(Optional.of(config));
        when(gateway.embed(config, "内容", "ollama", Duration.ofSeconds(60)))
                .thenReturn(new float[]{1, 2});
        var service = new AiEmbeddingService(repository, gateway,
                new AiProperties("", Duration.ofSeconds(60)), credentialCipher);

        assertThatThrownBy(() -> service.embedAll(List.of("内容")))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ErrorCode.AI_EMBEDDING_DIMENSION_MISMATCH));
    }

    private AiModelConfig embeddingConfig(int dimensions) {
        OffsetDateTime now = OffsetDateTime.now();
        return new AiModelConfig(1L, "本地向量模型", AiModelType.EMBEDDING, AiProvider.OLLAMA,
                "http://127.0.0.1:11434/v1", "bge-m3", null, BigDecimal.ZERO, 2048,
                dimensions, true, true, 1L, 1L, now, now);
    }
}

package com.smartmall.tlai.web.ai.dto;

import com.smartmall.tlai.ai.model.AiModelConfig;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AiModelConfigResponse(
        long id,
        String name,
        String modelType,
        String provider,
        String baseUrl,
        String modelName,
        BigDecimal temperature,
        int maxTokens,
        Integer embeddingDimensions,
        boolean active,
        boolean enabled,
        boolean credentialConfigured,
        boolean credentialRequired,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static AiModelConfigResponse from(AiModelConfig config) {
        return new AiModelConfigResponse(config.id(), config.name(), config.modelType().name(),
                config.provider().name(), config.baseUrl(), config.modelName(), config.temperature(), config.maxTokens(),
                config.embeddingDimensions(), config.active(), config.enabled(), config.credentialConfigured(),
                config.credentialRequired(), config.createdAt(), config.updatedAt());
    }
}

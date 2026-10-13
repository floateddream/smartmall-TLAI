package com.smartmall.tlai.ai.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AiModelConfig(
        Long id,
        String name,
        AiModelType modelType,
        AiProvider provider,
        String baseUrl,
        String modelName,
        String apiKeyCiphertext,
        BigDecimal temperature,
        int maxTokens,
        Integer embeddingDimensions,
        boolean active,
        boolean enabled,
        Long ownerUserId,
        Long createdBy,
        Long updatedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public boolean credentialConfigured() {
        return apiKeyCiphertext != null && !apiKeyCiphertext.isBlank();
    }

    public boolean credentialRequired() {
        return provider.credentialRequired();
    }
}

package com.smartmall.tlai.ai.application;

import com.smartmall.tlai.ai.model.AiProvider;
import com.smartmall.tlai.ai.model.AiModelType;

import java.math.BigDecimal;

public record AiModelCommand(
        String name,
        AiModelType modelType,
        AiProvider provider,
        String baseUrl,
        String modelName,
        String apiKey,
        BigDecimal temperature,
        int maxTokens,
        Integer embeddingDimensions,
        long operatorId
) {
}

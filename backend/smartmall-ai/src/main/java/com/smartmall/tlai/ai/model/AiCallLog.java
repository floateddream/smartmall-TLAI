package com.smartmall.tlai.ai.model;

import java.time.OffsetDateTime;

/**
 * 一次模型调用的安全元数据。计数仅保存数量与字符规模，不包含 API Key、提示词或模型回答。
 */
public record AiCallLog(
        Long id,
        Long modelConfigId,
        AiModelType modelType,
        AiProvider provider,
        String modelName,
        AiCallScene scene,
        AiCallStatus status,
        long latencyMs,
        int inputCount,
        long inputCharacterCount,
        int outputCount,
        Long outputUnitCount,
        String errorCode,
        String traceId,
        Long conversationId,
        OffsetDateTime createdAt
) {
}

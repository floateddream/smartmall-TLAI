package com.smartmall.tlai.web.ai.dto;

import com.smartmall.tlai.ai.model.AiCallLog;

import java.time.OffsetDateTime;

public record AiCallLogResponse(
        long id,
        Long modelConfigId,
        String modelType,
        String provider,
        String modelName,
        String scene,
        String status,
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
    public static AiCallLogResponse from(AiCallLog log) {
        return new AiCallLogResponse(log.id(), log.modelConfigId(), log.modelType().name(),
                log.provider().name(), log.modelName(), log.scene().name(), log.status().name(),
                log.latencyMs(), log.inputCount(), log.inputCharacterCount(), log.outputCount(),
                log.outputUnitCount(), log.errorCode(), log.traceId(), log.conversationId(), log.createdAt());
    }
}

package com.smartmall.tlai.ai.application;

import com.smartmall.tlai.ai.model.AiCallLog;
import com.smartmall.tlai.ai.model.AiCallContext;
import com.smartmall.tlai.ai.model.AiCallScene;
import com.smartmall.tlai.ai.model.AiCallStatus;
import com.smartmall.tlai.ai.model.AiModelConfig;
import com.smartmall.tlai.ai.model.AiModelType;
import com.smartmall.tlai.ai.model.AiProvider;
import com.smartmall.tlai.ai.repository.AiCallLogRepository;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
public class AiCallLogService {
    private final AiCallLogRepository repository;

    public AiCallLogService(AiCallLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AiModelConfig config, AiCallScene scene, AiCallStatus status, long latencyMs,
                       int inputCount, long inputCharacterCount, int outputCount,
                       Long outputUnitCount, String errorCode) {
        record(config, scene, status, latencyMs, inputCount, inputCharacterCount,
                outputCount, outputUnitCount, errorCode, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AiModelConfig config, AiCallScene scene, AiCallStatus status, long latencyMs,
                       int inputCount, long inputCharacterCount, int outputCount,
                       Long outputUnitCount, String errorCode, AiCallContext context) {
        repository.insert(new AiCallLog(null, config.id(), config.modelType(), config.provider(), config.modelName(),
                scene, status, Math.max(latencyMs, 0), Math.max(inputCount, 0),
                Math.max(inputCharacterCount, 0), Math.max(outputCount, 0),
                outputUnitCount == null ? null : Math.max(outputUnitCount, 0),
                normalizeErrorCode(status, errorCode), context == null ? null : context.traceId(),
                context == null ? null : context.conversationId(), OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Transactional(readOnly = true)
    public PageResponse<AiCallLog> list(int page, int size, String modelType, String provider,
                                        String scene, String status) {
        return list(page, size, modelType, provider, scene, status, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<AiCallLog> list(int page, int size, String modelType, String provider,
                                        String scene, String status, String traceId) {
        AiModelType normalizedType = enumFilter(modelType, AiModelType.class, "模型类型");
        AiProvider normalizedProvider = enumFilter(provider, AiProvider.class, "供应商");
        AiCallScene normalizedScene = enumFilter(scene, AiCallScene.class, "调用场景");
        AiCallStatus normalizedStatus = enumFilter(status, AiCallStatus.class, "调用状态");
        String normalizedTraceId = normalizeTraceId(traceId);
        return new PageResponse<>(repository.findPage(page, size, normalizedType, normalizedProvider,
                normalizedScene, normalizedStatus, normalizedTraceId), repository.count(normalizedType,
                normalizedProvider, normalizedScene, normalizedStatus, normalizedTraceId), page, size);
    }

    @Transactional(readOnly = true)
    public AiCallLog get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "AI 调用日志不存在"));
    }

    public static String errorCode(RuntimeException exception) {
        if (exception instanceof BusinessException businessException) {
            return businessException.getErrorCode().code();
        }
        return ErrorCode.AI_PROVIDER_ERROR.code();
    }

    private static String normalizeErrorCode(AiCallStatus status, String errorCode) {
        if (status == AiCallStatus.SUCCESS) return null;
        if (errorCode == null || errorCode.isBlank()) return ErrorCode.AI_PROVIDER_ERROR.code();
        String value = errorCode.trim().toUpperCase(Locale.ROOT);
        return value.length() <= 80 ? value : value.substring(0, 80);
    }

    private static <T extends Enum<T>> T enumFilter(String value, Class<T> type, String label) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + "不合法");
        }
    }

    private static String normalizeTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) return null;
        String normalized = traceId.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "追踪标识不合法");
        }
        return normalized;
    }
}

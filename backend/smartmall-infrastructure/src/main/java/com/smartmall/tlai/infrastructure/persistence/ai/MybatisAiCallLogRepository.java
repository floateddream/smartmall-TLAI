package com.smartmall.tlai.infrastructure.persistence.ai;

import com.smartmall.tlai.ai.model.AiCallLog;
import com.smartmall.tlai.ai.model.AiCallScene;
import com.smartmall.tlai.ai.model.AiCallStatus;
import com.smartmall.tlai.ai.model.AiModelType;
import com.smartmall.tlai.ai.model.AiProvider;
import com.smartmall.tlai.ai.repository.AiCallLogRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisAiCallLogRepository implements AiCallLogRepository {
    private final AiCallLogMapper mapper;

    public MybatisAiCallLogRepository(AiCallLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void insert(AiCallLog callLog) {
        mapper.insert(toEntity(callLog));
    }

    @Override
    public long count(AiModelType modelType, AiProvider provider, AiCallScene scene, AiCallStatus status,
                      String traceId) {
        return mapper.countLogs(name(modelType), name(provider), name(scene), name(status), traceId);
    }

    @Override
    public List<AiCallLog> findPage(int page, int size, AiModelType modelType, AiProvider provider,
                                    AiCallScene scene, AiCallStatus status, String traceId) {
        return mapper.selectPage(((long) page - 1) * size, size, name(modelType), name(provider),
                name(scene), name(status), traceId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<AiCallLog> findById(long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    private AiCallLogPersistenceEntity toEntity(AiCallLog log) {
        AiCallLogPersistenceEntity entity = new AiCallLogPersistenceEntity();
        entity.setId(log.id());
        entity.setModelConfigId(log.modelConfigId());
        entity.setModelType(log.modelType().name());
        entity.setProvider(log.provider().name());
        entity.setModelName(log.modelName());
        entity.setScene(log.scene().name());
        entity.setStatus(log.status().name());
        entity.setLatencyMs(log.latencyMs());
        entity.setInputCount(log.inputCount());
        entity.setInputCharacterCount(log.inputCharacterCount());
        entity.setOutputCount(log.outputCount());
        entity.setOutputUnitCount(log.outputUnitCount());
        entity.setErrorCode(log.errorCode());
        entity.setTraceId(log.traceId());
        entity.setConversationId(log.conversationId());
        entity.setCreatedAt(log.createdAt());
        return entity;
    }

    private AiCallLog toDomain(AiCallLogPersistenceEntity entity) {
        return new AiCallLog(entity.getId(), entity.getModelConfigId(),
                AiModelType.valueOf(entity.getModelType()), AiProvider.valueOf(entity.getProvider()),
                entity.getModelName(), AiCallScene.valueOf(entity.getScene()), AiCallStatus.valueOf(entity.getStatus()),
                entity.getLatencyMs(), entity.getInputCount(), entity.getInputCharacterCount(),
                entity.getOutputCount(), entity.getOutputUnitCount(), entity.getErrorCode(),
                entity.getTraceId(), entity.getConversationId(), entity.getCreatedAt());
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }
}

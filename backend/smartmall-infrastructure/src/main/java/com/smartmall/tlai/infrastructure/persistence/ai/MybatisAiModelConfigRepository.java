package com.smartmall.tlai.infrastructure.persistence.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartmall.tlai.ai.model.AiModelConfig;
import com.smartmall.tlai.ai.model.AiModelType;
import com.smartmall.tlai.ai.model.AiProvider;
import com.smartmall.tlai.ai.repository.AiModelConfigRepository;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MybatisAiModelConfigRepository implements AiModelConfigRepository {
    private final AiModelConfigMapper mapper;

    public MybatisAiModelConfigRepository(AiModelConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<AiModelConfig> findAll() {
        return mapper.selectList(Wrappers.<AiModelConfigPersistenceEntity>lambdaQuery()
                        .orderByDesc(AiModelConfigPersistenceEntity::getActive)
                        .orderByDesc(AiModelConfigPersistenceEntity::getUpdatedAt))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<AiModelConfig> findById(long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<AiModelConfig> findByName(String name) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<AiModelConfigPersistenceEntity>lambdaQuery()
                        .eq(AiModelConfigPersistenceEntity::getName, name)))
                .map(this::toDomain);
    }

    @Override
    public Optional<AiModelConfig> findActive(AiModelType modelType) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<AiModelConfigPersistenceEntity>lambdaQuery()
                        .eq(AiModelConfigPersistenceEntity::getModelType, modelType.name())
                        .eq(AiModelConfigPersistenceEntity::getActive, true)
                        .eq(AiModelConfigPersistenceEntity::getEnabled, true)))
                .map(this::toDomain);
    }

    @Override
    public AiModelConfig insert(AiModelConfig config) {
        AiModelConfigPersistenceEntity entity = toEntity(config);
        try {
            mapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.AI_MODEL_NAME_EXISTS);
        }
        return toDomain(entity);
    }

    @Override
    public AiModelConfig update(AiModelConfig config) {
        AiModelConfigPersistenceEntity entity = toEntity(config);
        try {
            mapper.updateById(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.AI_MODEL_NAME_EXISTS);
        }
        return config;
    }

    @Override
    public void lockConfiguration() {
        mapper.lockConfiguration();
    }

    @Override
    public void clearActive(AiModelType modelType, OffsetDateTime changedAt, long operatorId) {
        mapper.clearActive(modelType, changedAt, operatorId);
    }

    @Override
    public boolean updateActive(long id, boolean active, OffsetDateTime changedAt, long operatorId) {
        return mapper.updateActive(id, active, changedAt, operatorId) == 1;
    }

    @Override
    public boolean updateEnabled(long id, boolean enabled, OffsetDateTime changedAt, long operatorId) {
        return mapper.updateEnabled(id, enabled, changedAt, operatorId) == 1;
    }

    private AiModelConfig toDomain(AiModelConfigPersistenceEntity entity) {
        return new AiModelConfig(entity.getId(), entity.getName(), AiModelType.valueOf(entity.getModelType()),
                AiProvider.valueOf(entity.getProvider()), entity.getBaseUrl(),
                entity.getModelName(), entity.getApiKeyCiphertext(), entity.getTemperature(), entity.getMaxTokens(),
                entity.getEmbeddingDimensions(),
                Boolean.TRUE.equals(entity.getActive()), Boolean.TRUE.equals(entity.getEnabled()),
                entity.getCreatedBy(), entity.getUpdatedBy(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private AiModelConfigPersistenceEntity toEntity(AiModelConfig config) {
        AiModelConfigPersistenceEntity entity = new AiModelConfigPersistenceEntity();
        entity.setId(config.id());
        entity.setName(config.name());
        entity.setModelType(config.modelType().name());
        entity.setProvider(config.provider().name());
        entity.setBaseUrl(config.baseUrl());
        entity.setModelName(config.modelName());
        entity.setApiKeyCiphertext(config.apiKeyCiphertext());
        entity.setTemperature(config.temperature());
        entity.setMaxTokens(config.maxTokens());
        entity.setEmbeddingDimensions(config.embeddingDimensions());
        entity.setActive(config.active());
        entity.setEnabled(config.enabled());
        entity.setCreatedBy(config.createdBy());
        entity.setUpdatedBy(config.updatedBy());
        entity.setCreatedAt(config.createdAt());
        entity.setUpdatedAt(config.updatedAt());
        return entity;
    }
}

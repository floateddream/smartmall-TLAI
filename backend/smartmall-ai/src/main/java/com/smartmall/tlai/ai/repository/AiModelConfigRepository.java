package com.smartmall.tlai.ai.repository;

import com.smartmall.tlai.ai.model.AiModelConfig;
import com.smartmall.tlai.ai.model.AiModelType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface AiModelConfigRepository {
    List<AiModelConfig> findSystem();

    List<AiModelConfig> findByOwner(long ownerUserId);

    Optional<AiModelConfig> findById(long id);

    Optional<AiModelConfig> findByName(String name, Long ownerUserId);

    Optional<AiModelConfig> findActiveSystem(AiModelType modelType);

    Optional<AiModelConfig> findActiveForOwner(AiModelType modelType, long ownerUserId);

    AiModelConfig insert(AiModelConfig config);

    AiModelConfig update(AiModelConfig config);

    void lockConfiguration();

    void clearActive(AiModelType modelType, Long ownerUserId, OffsetDateTime changedAt, long operatorId);

    boolean updateActive(long id, boolean active, OffsetDateTime changedAt, long operatorId);

    boolean updateEnabled(long id, boolean enabled, OffsetDateTime changedAt, long operatorId);
}

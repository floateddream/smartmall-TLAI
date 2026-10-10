package com.smartmall.tlai.ai.repository;

import com.smartmall.tlai.ai.model.AiCallLog;
import com.smartmall.tlai.ai.model.AiCallScene;
import com.smartmall.tlai.ai.model.AiCallStatus;
import com.smartmall.tlai.ai.model.AiModelType;
import com.smartmall.tlai.ai.model.AiProvider;

import java.util.List;
import java.util.Optional;

public interface AiCallLogRepository {
    void insert(AiCallLog callLog);

    long count(AiModelType modelType, AiProvider provider, AiCallScene scene, AiCallStatus status,
               String traceId);

    List<AiCallLog> findPage(int page, int size, AiModelType modelType, AiProvider provider,
                             AiCallScene scene, AiCallStatus status, String traceId);

    Optional<AiCallLog> findById(long id);
}

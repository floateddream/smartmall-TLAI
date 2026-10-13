package com.smartmall.tlai.ai.application;

import com.smartmall.tlai.ai.config.AiProperties;
import com.smartmall.tlai.ai.gateway.AiEmbeddingGateway;
import com.smartmall.tlai.ai.model.AiCallScene;
import com.smartmall.tlai.ai.model.AiCallStatus;
import com.smartmall.tlai.ai.model.AiModelConfig;
import com.smartmall.tlai.ai.model.AiModelType;
import com.smartmall.tlai.ai.repository.AiModelConfigRepository;
import com.smartmall.tlai.ai.security.AiCredentialCipher;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 为知识库等内部用例提供活动 Embedding 模型，不向外暴露模型密钥。 */
@Service
public class AiEmbeddingService {
    private static final Logger log = LoggerFactory.getLogger(AiEmbeddingService.class);

    private final AiModelConfigRepository repository;
    private final AiEmbeddingGateway gateway;
    private final AiProperties properties;
    private final AiCredentialCipher credentialCipher;
    private final AiCallLogService callLogService;

    public AiEmbeddingService(AiModelConfigRepository repository, AiEmbeddingGateway gateway,
                              AiProperties properties, AiCredentialCipher credentialCipher,
                              AiCallLogService callLogService) {
        this.repository = repository;
        this.gateway = gateway;
        this.properties = properties;
        this.credentialCipher = credentialCipher;
        this.callLogService = callLogService;
    }

    public List<float[]> embedAll(List<String> inputs) {
        if (inputs == null || inputs.isEmpty()) return List.of();
        if (inputs.stream().anyMatch(input -> input == null || input.isBlank())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Embedding 输入不能为空");
        }

        AiModelConfig config = repository.findActiveSystem(AiModelType.EMBEDDING)
                .filter(AiModelConfig::enabled)
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_MODEL_NOT_CONFIGURED,
                        "尚未启用可用的 Embedding 模型"));
        log.info("AI 向量批处理开始：配置ID={}，供应商={}，模型={}，输入数量={}",
                config.id(), config.provider(), config.modelName(), inputs.size());

        long startedAt = System.nanoTime();
        List<float[]> vectors = new ArrayList<>(inputs.size());
        try {
            String apiKey = config.credentialRequired()
                    ? credentialCipher.decrypt(config.apiKeyCiphertext()) : "ollama";
            for (String input : inputs) {
                float[] vector = gateway.embed(config, input, apiKey, properties.requestTimeout());
                if (vector.length != config.embeddingDimensions()) {
                    throw new BusinessException(ErrorCode.AI_EMBEDDING_DIMENSION_MISMATCH,
                            "配置维度为 %d，但模型实际返回 %d 维"
                                    .formatted(config.embeddingDimensions(), vector.length));
                }
                vectors.add(vector);
            }
        } catch (RuntimeException exception) {
            recordSafely(config, AiCallStatus.FAILED, elapsedMs(startedAt), inputs,
                    vectors.size(), outputUnits(vectors), AiCallLogService.errorCode(exception));
            throw exception;
        }
        recordSafely(config, AiCallStatus.SUCCESS, elapsedMs(startedAt), inputs,
                vectors.size(), outputUnits(vectors), null);
        log.info("AI 向量批处理成功：配置ID={}，模型={}，输入数量={}，向量维度={}",
                config.id(), config.modelName(), vectors.size(), config.embeddingDimensions());
        return List.copyOf(vectors);
    }

    private void recordSafely(AiModelConfig config, AiCallStatus status, long latencyMs, List<String> inputs,
                              int outputCount, long outputUnitCount, String errorCode) {
        try {
            callLogService.record(config, AiCallScene.KNOWLEDGE_EMBEDDING, status, latencyMs, inputs.size(),
                    inputs.stream().mapToLong(String::length).sum(), outputCount, outputUnitCount, errorCode);
        } catch (RuntimeException exception) {
            log.error("AI 调用日志保存失败：配置ID={}，类型=EMBEDDING，状态={}",
                    config.id(), status, exception);
        }
    }

    private static long outputUnits(List<float[]> vectors) {
        return vectors.stream().mapToLong(vector -> vector.length).sum();
    }

    private static long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}

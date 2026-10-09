package com.smartmall.tlai.ai.application;

import com.smartmall.tlai.ai.config.AiProperties;
import com.smartmall.tlai.ai.gateway.AiEmbeddingGateway;
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

    public AiEmbeddingService(AiModelConfigRepository repository, AiEmbeddingGateway gateway,
                              AiProperties properties, AiCredentialCipher credentialCipher) {
        this.repository = repository;
        this.gateway = gateway;
        this.properties = properties;
        this.credentialCipher = credentialCipher;
    }

    public List<float[]> embedAll(List<String> inputs) {
        if (inputs == null || inputs.isEmpty()) return List.of();
        if (inputs.stream().anyMatch(input -> input == null || input.isBlank())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Embedding 输入不能为空");
        }

        AiModelConfig config = repository.findActive(AiModelType.EMBEDDING)
                .filter(AiModelConfig::enabled)
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_MODEL_NOT_CONFIGURED,
                        "尚未启用可用的 Embedding 模型"));
        String apiKey = config.credentialRequired()
                ? credentialCipher.decrypt(config.apiKeyCiphertext()) : "ollama";
        log.info("AI embedding batch started: configId={}, provider={}, model={}, inputCount={}",
                config.id(), config.provider(), config.modelName(), inputs.size());

        List<float[]> vectors = new ArrayList<>(inputs.size());
        for (String input : inputs) {
            float[] vector = gateway.embed(config, input, apiKey, properties.requestTimeout());
            if (vector.length != config.embeddingDimensions()) {
                throw new BusinessException(ErrorCode.AI_EMBEDDING_DIMENSION_MISMATCH,
                        "配置维度为 %d，但模型实际返回 %d 维"
                                .formatted(config.embeddingDimensions(), vector.length));
            }
            vectors.add(vector);
        }
        log.info("AI embedding batch succeeded: configId={}, model={}, inputCount={}, dimensions={}",
                config.id(), config.modelName(), vectors.size(), config.embeddingDimensions());
        return List.copyOf(vectors);
    }
}

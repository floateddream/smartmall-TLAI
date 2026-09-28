package com.smartmall.tlai.ai.gateway;

import com.smartmall.tlai.ai.model.AiModelConfig;

import java.time.Duration;

public interface AiEmbeddingGateway {
    float[] embed(AiModelConfig config, String input, String apiKey, Duration timeout);
}

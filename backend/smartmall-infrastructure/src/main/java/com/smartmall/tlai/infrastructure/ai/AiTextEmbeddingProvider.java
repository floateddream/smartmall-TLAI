package com.smartmall.tlai.infrastructure.ai;

import com.smartmall.tlai.ai.application.AiEmbeddingService;
import com.smartmall.tlai.knowledge.processing.TextEmbeddingProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/** 连接知识库端口与 AI 模块，避免 knowledge 模块反向依赖具体模型实现。 */
@Component
public class AiTextEmbeddingProvider implements TextEmbeddingProvider {
    private final AiEmbeddingService embeddingService;

    public AiTextEmbeddingProvider(AiEmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @Override
    public List<float[]> embedAll(List<String> inputs) {
        return embeddingService.embedAll(inputs);
    }
}

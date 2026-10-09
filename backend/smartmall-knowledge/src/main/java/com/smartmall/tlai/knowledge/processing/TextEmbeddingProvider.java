package com.smartmall.tlai.knowledge.processing;

import java.util.List;

/** 将知识片段转换为向量，知识库模块不感知具体模型供应商。 */
public interface TextEmbeddingProvider {
    List<float[]> embedAll(List<String> inputs);
}

package com.smartmall.tlai.knowledge.model;

/** 一个可独立检索的文本片段及其向量。 */
public record KnowledgeChunk(
        Long id,
        long documentId,
        int chunkIndex,
        String content,
        int tokenCount,
        float[] embedding) {
}

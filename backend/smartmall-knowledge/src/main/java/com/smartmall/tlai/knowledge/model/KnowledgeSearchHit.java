package com.smartmall.tlai.knowledge.model;

/** 一条向量检索结果，score 越接近 1 表示与问题语义越相似。 */
public record KnowledgeSearchHit(
        long chunkId,
        long documentId,
        String documentName,
        int chunkIndex,
        String content,
        double score
) {
}

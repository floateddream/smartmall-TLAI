package com.smartmall.tlai.knowledge.repository;

import com.smartmall.tlai.knowledge.model.KnowledgeChunk;
import com.smartmall.tlai.knowledge.model.KnowledgeSearchHit;

import java.util.List;

/** 文档分段与向量的持久化端口。 */
public interface KnowledgeChunkRepository {
    void deleteByDocumentId(long documentId);
    void insertAll(List<KnowledgeChunk> chunks);
    List<KnowledgeSearchHit> searchSimilar(float[] queryVector, int limit, double minScore);
}

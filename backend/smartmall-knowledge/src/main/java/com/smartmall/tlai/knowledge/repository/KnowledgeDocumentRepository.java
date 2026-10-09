package com.smartmall.tlai.knowledge.repository;

import com.smartmall.tlai.knowledge.model.KnowledgeDocument;
import com.smartmall.tlai.knowledge.model.KnowledgeDocumentStatus;

import java.util.List;
import java.util.Optional;

/** 文档元数据持久化端口，MyBatis 实现位于 infrastructure 模块。 */
public interface KnowledgeDocumentRepository {
    long count(String keyword, KnowledgeDocumentStatus status);
    List<KnowledgeDocument> findPage(int page, int size, String keyword, KnowledgeDocumentStatus status);
    Optional<KnowledgeDocument> findById(long id);
    Optional<KnowledgeDocument> claimNextUploaded();
    KnowledgeDocument insert(KnowledgeDocument document);
    boolean markReady(long id, int chunkCount, java.time.OffsetDateTime updatedAt);
    boolean markFailed(long id, String errorMessage, java.time.OffsetDateTime updatedAt);
    boolean retryFailed(long id, java.time.OffsetDateTime updatedAt);
    boolean deleteById(long id);
}

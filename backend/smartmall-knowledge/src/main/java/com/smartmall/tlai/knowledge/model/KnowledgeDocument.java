package com.smartmall.tlai.knowledge.model;

import java.time.OffsetDateTime;

/** 知识库文档元数据；原始文件内容保存在对象存储中。 */
public record KnowledgeDocument(
        Long id,
        String originalName,
        String objectKey,
        String contentType,
        long sizeBytes,
        String fileExtension,
        KnowledgeDocumentStatus status,
        int chunkCount,
        String errorMessage,
        long uploadedBy,
        String uploaderName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}

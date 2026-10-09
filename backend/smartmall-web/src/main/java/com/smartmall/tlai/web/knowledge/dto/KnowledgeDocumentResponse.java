package com.smartmall.tlai.web.knowledge.dto;

import com.smartmall.tlai.knowledge.model.KnowledgeDocument;
import com.smartmall.tlai.knowledge.model.KnowledgeDocumentStatus;

import java.time.OffsetDateTime;

public record KnowledgeDocumentResponse(
        Long id,
        String originalName,
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
    public static KnowledgeDocumentResponse from(KnowledgeDocument document) {
        return new KnowledgeDocumentResponse(document.id(), document.originalName(), document.contentType(),
                document.sizeBytes(), document.fileExtension(), document.status(), document.chunkCount(),
                document.errorMessage(), document.uploadedBy(), document.uploaderName(),
                document.createdAt(), document.updatedAt());
    }
}

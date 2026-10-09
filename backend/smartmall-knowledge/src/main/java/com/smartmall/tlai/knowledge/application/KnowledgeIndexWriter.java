package com.smartmall.tlai.knowledge.application;

import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import com.smartmall.tlai.knowledge.model.KnowledgeChunk;
import com.smartmall.tlai.knowledge.repository.KnowledgeChunkRepository;
import com.smartmall.tlai.knowledge.repository.KnowledgeDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/** 在一个数据库事务中替换分段并推进文档状态，避免出现半份索引。 */
@Service
public class KnowledgeIndexWriter {
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;

    public KnowledgeIndexWriter(KnowledgeDocumentRepository documentRepository,
                                KnowledgeChunkRepository chunkRepository) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    @Transactional
    public void complete(long documentId, List<KnowledgeChunk> chunks) {
        chunkRepository.deleteByDocumentId(documentId);
        chunkRepository.insertAll(chunks);
        if (!documentRepository.markReady(documentId, chunks.size(), OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_PROCESSING_STATE_INVALID);
        }
    }

    @Transactional
    public void fail(long documentId, String errorMessage) {
        if (!documentRepository.markFailed(documentId, errorMessage, OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_PROCESSING_STATE_INVALID);
        }
    }
}

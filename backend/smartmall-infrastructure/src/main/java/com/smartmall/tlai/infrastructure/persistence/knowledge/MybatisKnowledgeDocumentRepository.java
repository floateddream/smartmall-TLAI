package com.smartmall.tlai.infrastructure.persistence.knowledge;

import com.smartmall.tlai.knowledge.model.KnowledgeDocument;
import com.smartmall.tlai.knowledge.model.KnowledgeDocumentStatus;
import com.smartmall.tlai.knowledge.repository.KnowledgeDocumentRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisKnowledgeDocumentRepository implements KnowledgeDocumentRepository {
    private final KnowledgeDocumentMapper mapper;

    public MybatisKnowledgeDocumentRepository(KnowledgeDocumentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public long count(String keyword, KnowledgeDocumentStatus status) {
        return mapper.countFiltered(keyword, status == null ? null : status.name());
    }

    @Override
    public List<KnowledgeDocument> findPage(int page, int size, String keyword,
                                            KnowledgeDocumentStatus status) {
        long offset = ((long) page - 1) * size;
        return mapper.findPage(offset, size, keyword, status == null ? null : status.name())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<KnowledgeDocument> findById(long id) {
        return Optional.ofNullable(mapper.findDetailById(id)).map(this::toDomain);
    }

    @Override
    @Transactional
    public Optional<KnowledgeDocument> claimNextUploaded() {
        Long id = mapper.findNextUploadedIdForUpdate();
        if (id == null || mapper.markProcessing(id) != 1) return Optional.empty();
        return Optional.ofNullable(mapper.findDetailById(id)).map(this::toDomain);
    }

    @Override
    public KnowledgeDocument insert(KnowledgeDocument document) {
        KnowledgeDocumentPersistenceEntity entity = toEntity(document);
        mapper.insert(entity);
        return toDomain(mapper.findDetailById(entity.getId()));
    }

    @Override
    public boolean markReady(long id, int chunkCount, java.time.OffsetDateTime updatedAt) {
        return mapper.markReady(id, chunkCount, updatedAt) == 1;
    }

    @Override
    public boolean markFailed(long id, String errorMessage, java.time.OffsetDateTime updatedAt) {
        return mapper.markFailed(id, errorMessage, updatedAt) == 1;
    }

    @Override
    public boolean retryFailed(long id, java.time.OffsetDateTime updatedAt) {
        return mapper.retryFailed(id, updatedAt) == 1;
    }

    @Override
    public boolean deleteById(long id) {
        return mapper.deleteById(id) == 1;
    }

    private KnowledgeDocument toDomain(KnowledgeDocumentPersistenceEntity entity) {
        return new KnowledgeDocument(entity.getId(), entity.getOriginalName(), entity.getObjectKey(),
                entity.getContentType(), entity.getSizeBytes(), entity.getFileExtension(),
                KnowledgeDocumentStatus.valueOf(entity.getStatus()), entity.getChunkCount(),
                entity.getErrorMessage(), entity.getUploadedBy(), entity.getUploaderName(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private KnowledgeDocumentPersistenceEntity toEntity(KnowledgeDocument document) {
        KnowledgeDocumentPersistenceEntity entity = new KnowledgeDocumentPersistenceEntity();
        entity.setId(document.id());
        entity.setOriginalName(document.originalName());
        entity.setObjectKey(document.objectKey());
        entity.setContentType(document.contentType());
        entity.setSizeBytes(document.sizeBytes());
        entity.setFileExtension(document.fileExtension());
        entity.setStatus(document.status().name());
        entity.setChunkCount(document.chunkCount());
        entity.setErrorMessage(document.errorMessage());
        entity.setUploadedBy(document.uploadedBy());
        entity.setCreatedAt(document.createdAt());
        entity.setUpdatedAt(document.updatedAt());
        return entity;
    }
}

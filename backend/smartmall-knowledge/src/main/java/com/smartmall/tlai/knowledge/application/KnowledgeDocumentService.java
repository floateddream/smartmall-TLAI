package com.smartmall.tlai.knowledge.application;

import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import com.smartmall.tlai.knowledge.model.KnowledgeDocument;
import com.smartmall.tlai.knowledge.model.KnowledgeDocumentStatus;
import com.smartmall.tlai.knowledge.repository.KnowledgeDocumentRepository;
import com.smartmall.tlai.knowledge.storage.ObjectStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class KnowledgeDocumentService {
    public static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "txt", "md");
    private static final Logger log = LoggerFactory.getLogger(KnowledgeDocumentService.class);

    private final KnowledgeDocumentRepository repository;
    private final ObjectStorage objectStorage;

    public KnowledgeDocumentService(KnowledgeDocumentRepository repository, ObjectStorage objectStorage) {
        this.repository = repository;
        this.objectStorage = objectStorage;
    }

    @Transactional(readOnly = true)
    public PageResponse<KnowledgeDocument> list(int page, int size, String keyword,
                                                KnowledgeDocumentStatus status) {
        String search = keyword == null ? "" : keyword.trim();
        return new PageResponse<>(repository.findPage(page, size, search, status),
                repository.count(search, status), page, size);
    }

    @Transactional(readOnly = true)
    public KnowledgeDocument get(long id) {
        return requireDocument(id);
    }

    public KnowledgeDocument upload(String originalName, long sizeBytes,
                                    InputStream inputStream, long uploaderId) {
        String safeName = normalizeFilename(originalName);
        String extension = extensionOf(safeName);
        validateFile(extension, sizeBytes);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String objectKey = buildObjectKey(extension, now);
        String normalizedContentType = contentTypeFor(extension);
        objectStorage.put(objectKey, inputStream, sizeBytes, normalizedContentType);

        try {
            KnowledgeDocument document = new KnowledgeDocument(null, safeName, objectKey,
                    normalizedContentType, sizeBytes, extension, KnowledgeDocumentStatus.UPLOADED,
                    0, null, uploaderId, null, now, now);
            KnowledgeDocument saved = repository.insert(document);
            log.info("知识库文档上传成功：文档ID={}，对象路径={}，文件大小={}，上传人ID={}",
                    saved.id(), objectKey, sizeBytes, uploaderId);
            return saved;
        } catch (RuntimeException exception) {
            // 数据库写入失败时补偿删除对象，避免 MinIO 中留下无法管理的孤立文件。
            try {
                objectStorage.delete(objectKey);
            } catch (RuntimeException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
                log.error("清理知识库孤立对象失败：对象路径={}", objectKey, cleanupFailure);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public KnowledgeDocumentDownload download(long id) {
        KnowledgeDocument document = requireDocument(id);
        return new KnowledgeDocumentDownload(document, objectStorage.get(document.objectKey()));
    }

    public void delete(long id) {
        KnowledgeDocument document = requireDocument(id);
        objectStorage.delete(document.objectKey());
        if (!repository.deleteById(id)) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND);
        }
        log.info("知识库文档删除成功：文档ID={}，对象路径={}", id, document.objectKey());
    }

    public void retry(long id) {
        KnowledgeDocument document = requireDocument(id);
        if (document.status() != KnowledgeDocumentStatus.FAILED
                || !repository.retryFailed(id, OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_PROCESSING_STATE_INVALID,
                    "只有处理失败的文档可以重新处理");
        }
        log.info("知识库文档已加入重试队列：文档ID={}", id);
    }

    private KnowledgeDocument requireDocument(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND));
    }

    private void validateFile(String extension, long sizeBytes) {
        if (sizeBytes <= 0) throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_EMPTY);
        if (sizeBytes > MAX_FILE_SIZE) throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_TOO_LARGE);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_TYPE_UNSUPPORTED);
        }
    }

    private String normalizeFilename(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_TYPE_UNSUPPORTED);
        }
        String normalized = originalName.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (normalized.isBlank() || normalized.length() > 255) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件名不能为空且不能超过 255 个字符");
        }
        if (normalized.chars().anyMatch(Character::isISOControl)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件名不能包含控制字符");
        }
        return normalized;
    }

    private String extensionOf(String filename) {
        int index = filename.lastIndexOf('.');
        return index < 0 ? "" : filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String buildObjectKey(String extension, OffsetDateTime now) {
        String month = now.format(DateTimeFormatter.ofPattern("yyyy/MM"));
        return "documents/" + month + "/" + UUID.randomUUID() + "." + extension;
    }

    private String contentTypeFor(String extension) {
        // 浏览器上报的 Content-Type 可以伪造，下载响应统一使用后端认可的文件类型。
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "md" -> "text/markdown";
            default -> "text/plain";
        };
    }
}

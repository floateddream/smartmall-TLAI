package com.smartmall.tlai.knowledge.application;

import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import com.smartmall.tlai.knowledge.model.KnowledgeChunk;
import com.smartmall.tlai.knowledge.model.KnowledgeDocument;
import com.smartmall.tlai.knowledge.model.StoredObject;
import com.smartmall.tlai.knowledge.model.TextChunk;
import com.smartmall.tlai.knowledge.processing.DocumentTextExtractor;
import com.smartmall.tlai.knowledge.processing.TextEmbeddingProvider;
import com.smartmall.tlai.knowledge.repository.KnowledgeDocumentRepository;
import com.smartmall.tlai.knowledge.storage.ObjectStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class KnowledgeDocumentProcessor {
    static final int KNOWLEDGE_VECTOR_DIMENSIONS = 1024;
    static final int MAX_EXTRACTED_CHARACTERS = 200_000;
    private static final int MAX_ERROR_LENGTH = 900;
    private static final Logger log = LoggerFactory.getLogger(KnowledgeDocumentProcessor.class);

    private final KnowledgeDocumentRepository documentRepository;
    private final ObjectStorage objectStorage;
    private final DocumentTextExtractor textExtractor;
    private final KnowledgeTextSplitter textSplitter;
    private final TextEmbeddingProvider embeddingProvider;
    private final KnowledgeIndexWriter indexWriter;

    public KnowledgeDocumentProcessor(KnowledgeDocumentRepository documentRepository,
                                      ObjectStorage objectStorage,
                                      DocumentTextExtractor textExtractor,
                                      KnowledgeTextSplitter textSplitter,
                                      TextEmbeddingProvider embeddingProvider,
                                      KnowledgeIndexWriter indexWriter) {
        this.documentRepository = documentRepository;
        this.objectStorage = objectStorage;
        this.textExtractor = textExtractor;
        this.textSplitter = textSplitter;
        this.embeddingProvider = embeddingProvider;
        this.indexWriter = indexWriter;
    }

    /** 领取并处理一份文档；返回 false 表示当前没有待处理任务。 */
    public boolean processNext() {
        Optional<KnowledgeDocument> claimed = documentRepository.claimNextUploaded();
        if (claimed.isEmpty()) return false;
        processClaimed(claimed.get());
        return true;
    }

    private void processClaimed(KnowledgeDocument document) {
        log.info("知识库文档处理开始：文档ID={}，文件名={}", document.id(), document.originalName());
        try {
            StoredObject storedObject = objectStorage.get(document.objectKey());
            String text;
            try (InputStream inputStream = storedObject.inputStream()) {
                text = textExtractor.extract(inputStream, document.originalName());
            }
            if (text == null || text.isBlank()) {
                throw new BusinessException(ErrorCode.KNOWLEDGE_TEXT_EMPTY);
            }
            if (text.length() > MAX_EXTRACTED_CHARACTERS) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "文档提取文本超过 200000 个字符，请拆分后重新上传");
            }

            List<TextChunk> textChunks = textSplitter.split(text);
            if (textChunks.isEmpty()) throw new BusinessException(ErrorCode.KNOWLEDGE_TEXT_EMPTY);
            List<float[]> vectors = embeddingProvider.embedAll(
                    textChunks.stream().map(TextChunk::content).toList());
            if (vectors.size() != textChunks.size()) {
                throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR, "Embedding 返回数量与文本分段数量不一致");
            }

            List<KnowledgeChunk> chunks = new ArrayList<>(textChunks.size());
            for (int index = 0; index < textChunks.size(); index++) {
                float[] vector = vectors.get(index);
                if (vector.length != KNOWLEDGE_VECTOR_DIMENSIONS) {
                    throw new BusinessException(ErrorCode.AI_EMBEDDING_DIMENSION_MISMATCH,
                            "知识库要求 %d 维向量，但模型返回 %d 维"
                                    .formatted(KNOWLEDGE_VECTOR_DIMENSIONS, vector.length));
                }
                TextChunk textChunk = textChunks.get(index);
                chunks.add(new KnowledgeChunk(null, document.id(), textChunk.index(),
                        textChunk.content(), textChunk.tokenCount(), vector));
            }
            indexWriter.complete(document.id(), chunks);
            log.info("知识库文档处理成功：文档ID={}，分块数量={}",
                    document.id(), chunks.size());
        } catch (Exception exception) {
            String errorMessage = safeErrorMessage(exception);
            log.error("知识库文档处理失败：文档ID={}，文件名={}，失败原因={}",
                    document.id(), document.originalName(), errorMessage, exception);
            try {
                indexWriter.fail(document.id(), errorMessage);
            } catch (RuntimeException statusFailure) {
                exception.addSuppressed(statusFailure);
                log.error("保存知识库文档处理状态失败：文档ID={}", document.id(), statusFailure);
            }
        }
    }

    private String safeErrorMessage(Exception exception) {
        String message = exception instanceof BusinessException && exception.getMessage() != null
                ? exception.getMessage() : "文档处理失败，请查看服务日志";
        String normalized = message.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() <= MAX_ERROR_LENGTH ? normalized : normalized.substring(0, MAX_ERROR_LENGTH);
    }
}

package com.smartmall.tlai.knowledge.application;

import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import com.smartmall.tlai.knowledge.model.KnowledgeSearchHit;
import com.smartmall.tlai.knowledge.processing.TextEmbeddingProvider;
import com.smartmall.tlai.knowledge.repository.KnowledgeChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeSearchService {
    private static final int VECTOR_DIMENSIONS = 1024;
    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchService.class);

    private final TextEmbeddingProvider embeddingProvider;
    private final KnowledgeChunkRepository chunkRepository;

    public KnowledgeSearchService(TextEmbeddingProvider embeddingProvider,
                                  KnowledgeChunkRepository chunkRepository) {
        this.embeddingProvider = embeddingProvider;
        this.chunkRepository = chunkRepository;
    }

    /** 将问题转换为向量，再通过 pgvector 查找语义最接近的 READY 文档片段。 */
    public List<KnowledgeSearchHit> search(String query, int limit, double minScore) {
        if (query == null || query.isBlank() || query.length() > 4000) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "检索问题不能为空且不能超过 4000 个字符");
        }
        if (limit < 1 || limit > 10 || minScore < -1 || minScore > 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "知识库检索参数不合法");
        }

        long startedAt = System.nanoTime();
        List<float[]> vectors = embeddingProvider.embedAll(List.of(query.trim()));
        if (vectors.size() != 1 || vectors.getFirst().length != VECTOR_DIMENSIONS) {
            throw new BusinessException(ErrorCode.AI_EMBEDDING_DIMENSION_MISMATCH,
                    "知识库检索要求 1024 维问题向量");
        }
        List<KnowledgeSearchHit> hits = chunkRepository.searchSimilar(
                vectors.getFirst(), limit, minScore);
        log.info("Knowledge search completed: hitCount={}, limit={}, minScore={}, latencyMs={}",
                hits.size(), limit, minScore, (System.nanoTime() - startedAt) / 1_000_000);
        return hits;
    }
}

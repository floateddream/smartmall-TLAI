package com.smartmall.tlai.infrastructure.persistence.knowledge;

import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import com.smartmall.tlai.knowledge.model.KnowledgeChunk;
import com.smartmall.tlai.knowledge.model.KnowledgeSearchHit;
import com.smartmall.tlai.knowledge.repository.KnowledgeChunkRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MybatisKnowledgeChunkRepository implements KnowledgeChunkRepository {
    private final KnowledgeChunkMapper mapper;

    public MybatisKnowledgeChunkRepository(KnowledgeChunkMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void deleteByDocumentId(long documentId) {
        mapper.deleteByDocumentId(documentId);
    }

    @Override
    public void insertAll(List<KnowledgeChunk> chunks) {
        for (KnowledgeChunk chunk : chunks) {
            mapper.insert(chunk.documentId(), chunk.chunkIndex(), chunk.content(), chunk.tokenCount(),
                    vectorLiteral(chunk.embedding()));
        }
    }

    @Override
    public List<KnowledgeSearchHit> searchSimilar(float[] queryVector, int limit, double minScore) {
        return mapper.searchSimilar(vectorLiteral(queryVector), limit, minScore).stream()
                .map(row -> new KnowledgeSearchHit(row.getChunkId(), row.getDocumentId(),
                        row.getDocumentName(), row.getChunkIndex(), row.getContent(), row.getScore()))
                .toList();
    }

    private String vectorLiteral(float[] vector) {
        if (vector == null || vector.length == 0) {
            throw new BusinessException(ErrorCode.AI_EMBEDDING_DIMENSION_MISMATCH);
        }
        StringBuilder result = new StringBuilder(vector.length * 10).append('[');
        for (int index = 0; index < vector.length; index++) {
            if (!Float.isFinite(vector[index])) {
                throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR,
                        "Embedding 返回了无效数值");
            }
            if (index > 0) result.append(',');
            result.append(Float.toString(vector[index]));
        }
        return result.append(']').toString();
    }
}

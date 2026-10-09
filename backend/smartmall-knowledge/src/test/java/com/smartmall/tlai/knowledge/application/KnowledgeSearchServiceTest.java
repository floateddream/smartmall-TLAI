package com.smartmall.tlai.knowledge.application;

import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import com.smartmall.tlai.knowledge.model.KnowledgeSearchHit;
import com.smartmall.tlai.knowledge.processing.TextEmbeddingProvider;
import com.smartmall.tlai.knowledge.repository.KnowledgeChunkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeSearchServiceTest {
    @Mock TextEmbeddingProvider embeddingProvider;
    @Mock KnowledgeChunkRepository chunkRepository;

    @Test
    void embedsQuestionAndSearchesPgvectorRepository() {
        float[] vector = new float[1024];
        var hit = new KnowledgeSearchHit(1, 2, "退款规范.md", 0, "超过五千元需要审核", 0.82);
        when(embeddingProvider.embedAll(List.of("退款如何审核"))).thenReturn(List.of(vector));
        when(chunkRepository.searchSimilar(vector, 5, 0.35)).thenReturn(List.of(hit));

        List<KnowledgeSearchHit> result = service().search("退款如何审核", 5, 0.35);

        assertThat(result).containsExactly(hit);
        verify(chunkRepository).searchSimilar(vector, 5, 0.35);
    }

    @Test
    void rejectsEmbeddingWithWrongDimensions() {
        when(embeddingProvider.embedAll(any())).thenReturn(List.of(new float[3]));

        assertThatThrownBy(() -> service().search("退款", 5, 0.35))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ErrorCode.AI_EMBEDDING_DIMENSION_MISMATCH));
    }

    private KnowledgeSearchService service() {
        return new KnowledgeSearchService(embeddingProvider, chunkRepository);
    }
}

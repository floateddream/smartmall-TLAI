package com.smartmall.tlai.knowledge.application;

import com.smartmall.tlai.knowledge.model.KnowledgeDocument;
import com.smartmall.tlai.knowledge.model.KnowledgeDocumentStatus;
import com.smartmall.tlai.knowledge.model.StoredObject;
import com.smartmall.tlai.knowledge.processing.DocumentTextExtractor;
import com.smartmall.tlai.knowledge.processing.TextEmbeddingProvider;
import com.smartmall.tlai.knowledge.repository.KnowledgeDocumentRepository;
import com.smartmall.tlai.knowledge.storage.ObjectStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeDocumentProcessorTest {
    @Mock KnowledgeDocumentRepository documentRepository;
    @Mock ObjectStorage objectStorage;
    @Mock DocumentTextExtractor textExtractor;
    @Mock TextEmbeddingProvider embeddingProvider;
    @Mock KnowledgeIndexWriter indexWriter;

    @Test
    void processesClaimedDocumentIntoVectorChunks() {
        KnowledgeDocument document = processingDocument();
        when(documentRepository.claimNextUploaded()).thenReturn(Optional.of(document));
        when(objectStorage.get(document.objectKey())).thenReturn(new StoredObject(
                new ByteArrayInputStream("source".getBytes()), 6, "text/plain"));
        when(textExtractor.extract(any(), any())).thenReturn("退款超过五千元需要财务审核。");
        when(embeddingProvider.embedAll(any())).thenReturn(List.of(new float[1024]));
        var processor = processor();

        assertThat(processor.processNext()).isTrue();

        ArgumentCaptor<List<com.smartmall.tlai.knowledge.model.KnowledgeChunk>> chunks =
                ArgumentCaptor.forClass(List.class);
        verify(indexWriter).complete(anyLong(), chunks.capture());
        assertThat(chunks.getValue()).hasSize(1);
        assertThat(chunks.getValue().getFirst().embedding()).hasSize(1024);
        verify(indexWriter, never()).fail(anyLong(), any());
    }

    @Test
    void recordsFailureWhenExtractedTextIsEmpty() {
        KnowledgeDocument document = processingDocument();
        when(documentRepository.claimNextUploaded()).thenReturn(Optional.of(document));
        when(objectStorage.get(document.objectKey())).thenReturn(new StoredObject(
                new ByteArrayInputStream(new byte[0]), 1, "text/plain"));
        when(textExtractor.extract(any(), any())).thenReturn("  ");
        var processor = processor();

        assertThat(processor.processNext()).isTrue();

        verify(indexWriter).fail(document.id(), "文档中没有可处理的文本");
        verify(indexWriter, never()).complete(anyLong(), any());
    }

    @Test
    void returnsFalseWhenQueueIsEmpty() {
        when(documentRepository.claimNextUploaded()).thenReturn(Optional.empty());

        assertThat(processor().processNext()).isFalse();

        verify(objectStorage, never()).get(any());
    }

    private KnowledgeDocumentProcessor processor() {
        return new KnowledgeDocumentProcessor(documentRepository, objectStorage, textExtractor,
                new KnowledgeTextSplitter(), embeddingProvider, indexWriter);
    }

    private KnowledgeDocument processingDocument() {
        OffsetDateTime now = OffsetDateTime.now();
        return new KnowledgeDocument(11L, "refund.txt", "documents/refund.txt", "text/plain",
                100, "txt", KnowledgeDocumentStatus.PROCESSING, 0, null,
                7, "tester", now, now);
    }
}

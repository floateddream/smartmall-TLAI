package com.smartmall.tlai.knowledge.application;

import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import com.smartmall.tlai.knowledge.model.KnowledgeDocument;
import com.smartmall.tlai.knowledge.model.KnowledgeDocumentStatus;
import com.smartmall.tlai.knowledge.repository.KnowledgeDocumentRepository;
import com.smartmall.tlai.knowledge.storage.ObjectStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeDocumentServiceTest {
    @Mock KnowledgeDocumentRepository repository;
    @Mock ObjectStorage objectStorage;

    @Test
    void uploadsSupportedDocumentWithGeneratedObjectKey() {
        when(repository.insert(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        var service = new KnowledgeDocumentService(repository, objectStorage);
        byte[] content = "knowledge".getBytes();

        KnowledgeDocument result = service.upload("C:\\fakepath\\ handbook.PDF ",
                content.length, new ByteArrayInputStream(content), 7L);

        ArgumentCaptor<String> objectKey = ArgumentCaptor.forClass(String.class);
        verify(objectStorage).put(objectKey.capture(), any(), eq((long) content.length), eq("application/pdf"));
        assertThat(objectKey.getValue()).startsWith("documents/").endsWith(".pdf");
        assertThat(objectKey.getValue()).doesNotContain("handbook");
        assertThat(result.originalName()).isEqualTo("handbook.PDF");
        assertThat(result.fileExtension()).isEqualTo("pdf");
        assertThat(result.status()).isEqualTo(KnowledgeDocumentStatus.UPLOADED);
    }

    @Test
    void rejectsOversizedFileBeforeCallingStorage() {
        var service = new KnowledgeDocumentService(repository, objectStorage);

        assertThatThrownBy(() -> service.upload("large.pdf", KnowledgeDocumentService.MAX_FILE_SIZE + 1,
                new ByteArrayInputStream(new byte[0]), 7L))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.KNOWLEDGE_FILE_TOO_LARGE));
        verify(objectStorage, never()).put(any(), any(), anyLong(), any());
    }

    @Test
    void rejectsUnsupportedExtensionBeforeCallingStorage() {
        var service = new KnowledgeDocumentService(repository, objectStorage);

        assertThatThrownBy(() -> service.upload("script.exe", 10,
                new ByteArrayInputStream(new byte[10]), 7L))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ErrorCode.KNOWLEDGE_FILE_TYPE_UNSUPPORTED));
        verify(objectStorage, never()).put(any(), any(), anyLong(), any());
    }

    @Test
    void ignoresUntrustedBrowserContentType() {
        when(repository.insert(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        var service = new KnowledgeDocumentService(repository, objectStorage);

        service.upload("manual.pdf", 4, new ByteArrayInputStream("test".getBytes()), 7L);

        verify(objectStorage).put(any(), any(), eq(4L), eq("application/pdf"));
    }

    @Test
    void rejectsControlCharactersInFilename() {
        var service = new KnowledgeDocumentService(repository, objectStorage);

        assertThatThrownBy(() -> service.upload("manual\r\n.pdf", 4,
                new ByteArrayInputStream("test".getBytes()), 7L))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
        verify(objectStorage, never()).put(any(), any(), anyLong(), any());
    }

    @Test
    void deletesUploadedObjectWhenMetadataInsertFails() {
        when(repository.insert(any())).thenThrow(new IllegalStateException("database unavailable"));
        var service = new KnowledgeDocumentService(repository, objectStorage);

        assertThatThrownBy(() -> service.upload("guide.txt", 5,
                new ByteArrayInputStream("guide".getBytes()), 7L))
                .isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<String> objectKey = ArgumentCaptor.forClass(String.class);
        verify(objectStorage).delete(objectKey.capture());
        assertThat(objectKey.getValue()).endsWith(".txt");
    }

    @Test
    void rejectsDeletingMissingDocumentWithoutCallingStorage() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        var service = new KnowledgeDocumentService(repository, objectStorage);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND));
        verify(objectStorage, never()).delete(any());
    }

    @Test
    void queuesFailedDocumentForRetry() {
        KnowledgeDocument failed = withId(new KnowledgeDocument(null, "guide.txt", "documents/guide.txt",
                "text/plain", 5, "txt", KnowledgeDocumentStatus.FAILED, 0, "模型不可用",
                7L, "tester", OffsetDateTime.now(), OffsetDateTime.now()));
        when(repository.findById(1L)).thenReturn(Optional.of(failed));
        when(repository.retryFailed(eq(1L), any())).thenReturn(true);
        var service = new KnowledgeDocumentService(repository, objectStorage);

        service.retry(1L);

        verify(repository).retryFailed(eq(1L), any());
    }

    private KnowledgeDocument withId(KnowledgeDocument document) {
        OffsetDateTime now = document.createdAt();
        return new KnowledgeDocument(1L, document.originalName(), document.objectKey(), document.contentType(),
                document.sizeBytes(), document.fileExtension(), document.status(), document.chunkCount(),
                document.errorMessage(), document.uploadedBy(), "tester", now, now);
    }
}

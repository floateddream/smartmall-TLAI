package com.smartmall.tlai.web.knowledge;

import com.smartmall.tlai.common.api.ApiResponse;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import com.smartmall.tlai.knowledge.application.KnowledgeDocumentService;
import com.smartmall.tlai.knowledge.model.KnowledgeDocumentStatus;
import com.smartmall.tlai.security.AppUserPrincipal;
import com.smartmall.tlai.web.knowledge.dto.KnowledgeDocumentResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Validated
@RestController
@RequestMapping("/api/knowledge/documents")
public class KnowledgeDocumentController {
    private final KnowledgeDocumentService service;

    public KnowledgeDocumentController(KnowledgeDocumentService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('knowledge:view')")
    public ApiResponse<PageResponse<KnowledgeDocumentResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        var result = service.list(page, size, keyword, parseStatus(status));
        return ApiResponse.success(new PageResponse<>(result.items().stream()
                .map(KnowledgeDocumentResponse::from).toList(), result.total(), result.page(), result.size()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('knowledge:manage')")
    public ResponseEntity<ApiResponse<KnowledgeDocumentResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AppUserPrincipal principal) throws IOException {
        // MultipartFile 的临时资源由 Web 层持有，上传结束后在这里明确关闭读取流。
        try (InputStream inputStream = file.getInputStream()) {
            var document = service.upload(file.getOriginalFilename(), file.getSize(), inputStream, principal.id());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(KnowledgeDocumentResponse.from(document)));
        }
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('knowledge:view')")
    public ResponseEntity<InputStreamResource> download(@PathVariable @Min(1) long id) {
        var download = service.download(id);
        String contentType = download.object().contentType() == null
                ? download.document().contentType() : download.object().contentType();
        String disposition = ContentDisposition.attachment()
                .filename(download.document().originalName(), StandardCharsets.UTF_8)
                .build().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(download.object().sizeBytes())
                .body(new InputStreamResource(download.object().inputStream()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('knowledge:manage')")
    public ResponseEntity<Void> delete(@PathVariable @Min(1) long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAuthority('knowledge:manage')")
    public ResponseEntity<Void> retry(@PathVariable @Min(1) long id) {
        service.retry(id);
        return ResponseEntity.accepted().build();
    }

    private KnowledgeDocumentStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return KnowledgeDocumentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文档状态参数不正确");
        }
    }
}

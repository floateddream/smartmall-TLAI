package com.smartmall.tlai.web.analytics;

import com.smartmall.tlai.analytics.application.AnalyticsAuditService;
import com.smartmall.tlai.analytics.application.AnalyticsQueryService;
import com.smartmall.tlai.analytics.application.NaturalLanguageAnalyticsService;
import com.smartmall.tlai.analytics.model.AnalyticsQuery;
import com.smartmall.tlai.analytics.model.AnalyticsResult;
import com.smartmall.tlai.analytics.model.NaturalLanguageAnalyticsResult;
import com.smartmall.tlai.common.api.ApiResponse;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import com.smartmall.tlai.security.AppUserPrincipal;
import com.smartmall.tlai.web.analytics.dto.AnalyticsAuditResponse;
import com.smartmall.tlai.web.analytics.dto.AnalyticsQueryRequest;
import com.smartmall.tlai.web.analytics.dto.AnalyticsResultResponse;
import com.smartmall.tlai.web.analytics.dto.NaturalLanguageAnalyticsRequest;
import com.smartmall.tlai.web.analytics.dto.NaturalLanguageAnalyticsResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);
    private final AnalyticsQueryService service;
    private final NaturalLanguageAnalyticsService naturalLanguageService;
    private final AnalyticsAuditService auditService;

    public AnalyticsController(AnalyticsQueryService service,
                               NaturalLanguageAnalyticsService naturalLanguageService,
                               AnalyticsAuditService auditService) {
        this.service = service;
        this.naturalLanguageService = naturalLanguageService;
        this.auditService = auditService;
    }

    @PostMapping("/query")
    @PreAuthorize("hasAuthority('analytics:view')")
    public ApiResponse<AnalyticsResultResponse> query(@Valid @RequestBody AnalyticsQueryRequest request,
                                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        long startedAt = System.nanoTime();
        AnalyticsQuery query = request.toDomain();
        try {
            AnalyticsResult result = service.query(query);
            auditSuccess(principal, "STRUCTURED", null, result, null, elapsedMs(startedAt));
            return ApiResponse.success(AnalyticsResultResponse.from(result));
        } catch (RuntimeException exception) {
            auditFailure(principal, "STRUCTURED", null, query, exception, elapsedMs(startedAt));
            throw exception;
        }
    }

    @PostMapping("/natural-language")
    @PreAuthorize("hasAuthority('analytics:view')")
    public ApiResponse<NaturalLanguageAnalyticsResponse> naturalLanguage(
            @Valid @RequestBody NaturalLanguageAnalyticsRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        long startedAt = System.nanoTime();
        try {
            NaturalLanguageAnalyticsResult result = naturalLanguageService.analyze(request.question(), principal.id());
            auditSuccess(principal, "NATURAL_LANGUAGE", request.question(), result.analytics(),
                    result.modelName(), result.latencyMs());
            return ApiResponse.success(NaturalLanguageAnalyticsResponse.from(result));
        } catch (RuntimeException exception) {
            auditFailure(principal, "NATURAL_LANGUAGE", request.question(), null,
                    exception, elapsedMs(startedAt));
            throw exception;
        }
    }

    @GetMapping("/audits")
    @PreAuthorize("hasAuthority('analytics:audit_view')")
    public ApiResponse<PageResponse<AnalyticsAuditResponse>> audits(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String queryMode,
            @RequestParam(required = false) String status) {
        var result = auditService.list(page, size, queryMode, status);
        return ApiResponse.success(new PageResponse<>(result.items().stream()
                .map(AnalyticsAuditResponse::from).toList(), result.total(), result.page(), result.size()));
    }

    private void auditSuccess(AppUserPrincipal principal, String mode, String question,
                              AnalyticsResult result, String modelName, long latencyMs) {
        try {
            auditService.recordSuccess(principal.id(), principal.username(), mode, question,
                    result, modelName, latencyMs);
        } catch (RuntimeException exception) {
            log.error("数据分析成功审计记录保存失败：分析模式={}，用户ID={}",
                    mode, principal.id(), exception);
        }
    }

    private void auditFailure(AppUserPrincipal principal, String mode, String question,
                              AnalyticsQuery query, RuntimeException exception, long latencyMs) {
        try {
            String errorCode = exception instanceof BusinessException businessException
                    ? businessException.getErrorCode().code() : ErrorCode.INTERNAL_ERROR.code();
            auditService.recordFailure(principal.id(), principal.username(), mode, question,
                    query, errorCode, latencyMs);
        } catch (RuntimeException auditException) {
            log.error("数据分析失败审计记录保存失败：分析模式={}，用户ID={}",
                    mode, principal.id(), auditException);
        }
    }

    private static long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}

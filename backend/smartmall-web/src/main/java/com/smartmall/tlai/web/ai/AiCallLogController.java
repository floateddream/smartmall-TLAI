package com.smartmall.tlai.web.ai;

import com.smartmall.tlai.ai.application.AiCallLogService;
import com.smartmall.tlai.common.api.ApiResponse;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.web.ai.dto.AiCallLogResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/ai/call-logs")
public class AiCallLogController {
    private final AiCallLogService service;

    public AiCallLogController(AiCallLogService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ai:call_log_view')")
    public ApiResponse<PageResponse<AiCallLogResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String traceId) {
        var result = service.list(page, size, modelType, provider, scene, status, traceId);
        return ApiResponse.success(new PageResponse<>(result.items().stream()
                .map(AiCallLogResponse::from).toList(), result.total(), result.page(), result.size()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ai:call_log_view')")
    public ApiResponse<AiCallLogResponse> get(@PathVariable @Min(1) long id) {
        return ApiResponse.success(AiCallLogResponse.from(service.get(id)));
    }
}

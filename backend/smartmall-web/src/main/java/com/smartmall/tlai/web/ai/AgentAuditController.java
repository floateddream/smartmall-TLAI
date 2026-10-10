package com.smartmall.tlai.web.ai;

import com.smartmall.tlai.agent.application.AgentToolAuditService;
import com.smartmall.tlai.common.api.ApiResponse;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.web.ai.dto.AiAgentAuditResponse;
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
@RequestMapping("/api/ai/agent/audits")
public class AgentAuditController {
    private final AgentToolAuditService service;

    public AgentAuditController(AgentToolAuditService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('agent:audit_view')")
    public ApiResponse<PageResponse<AiAgentAuditResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String toolName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String traceId) {
        var result = service.list(page, size, toolName, status, traceId);
        return ApiResponse.success(new PageResponse<>(result.items().stream()
                .map(AiAgentAuditResponse::from).toList(), result.total(), result.page(), result.size()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('agent:audit_view')")
    public ApiResponse<AiAgentAuditResponse> get(@PathVariable @Min(1) long id) {
        return ApiResponse.success(AiAgentAuditResponse.from(service.get(id)));
    }
}

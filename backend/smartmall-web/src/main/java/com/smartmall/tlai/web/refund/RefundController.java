package com.smartmall.tlai.web.refund;

import com.smartmall.tlai.business.refund.application.CreateRefundCommand;
import com.smartmall.tlai.business.refund.application.RefundService;
import com.smartmall.tlai.business.refund.application.ReviewRefundCommand;
import com.smartmall.tlai.common.api.ApiResponse;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.security.AppUserPrincipal;
import com.smartmall.tlai.web.refund.dto.CreateRefundRequest;
import com.smartmall.tlai.web.refund.dto.RefundResponse;
import com.smartmall.tlai.web.refund.dto.ReviewRefundRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
public class RefundController {
    private final RefundService service;

    public RefundController(RefundService service) {
        this.service = service;
    }

    @GetMapping("/refunds")
    @PreAuthorize("hasAuthority('refund:view')")
    public ApiResponse<PageResponse<RefundResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        var result = service.list(page, size, keyword, status);
        return ApiResponse.success(new PageResponse<>(result.items().stream()
                .map(RefundResponse::from).toList(), result.total(), result.page(), result.size()));
    }

    @GetMapping("/refunds/{id}")
    @PreAuthorize("hasAuthority('refund:view')")
    public ApiResponse<RefundResponse> get(@PathVariable @Min(1) long id) {
        return ApiResponse.success(RefundResponse.from(service.get(id)));
    }

    @PostMapping("/orders/{orderId}/refunds")
    @PreAuthorize("hasAuthority('refund:apply')")
    public ResponseEntity<ApiResponse<RefundResponse>> create(
            @PathVariable @Min(1) long orderId,
            @Valid @RequestBody CreateRefundRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        var command = new CreateRefundCommand(orderId, principal.id(), principal.username(), request.reason());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(RefundResponse.from(service.create(command))));
    }

    @PreAuthorize("hasAuthority('refund:review')")
    @PostMapping("/refunds/{id}/approve")
    public ApiResponse<RefundResponse> approve(
            @PathVariable @Min(1) long id,
            @Valid @RequestBody ReviewRefundRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        var command = new ReviewRefundCommand(principal.id(), principal.username(), request.remark());
        return ApiResponse.success(RefundResponse.from(service.approve(id, command)));
    }

    @PreAuthorize("hasAuthority('refund:review')")
    @PostMapping("/refunds/{id}/reject")
    public ApiResponse<RefundResponse> reject(
            @PathVariable @Min(1) long id,
            @Valid @RequestBody ReviewRefundRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        var command = new ReviewRefundCommand(principal.id(), principal.username(), request.remark());
        return ApiResponse.success(RefundResponse.from(service.reject(id, command)));
    }
}

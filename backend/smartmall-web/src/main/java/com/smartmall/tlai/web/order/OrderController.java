package com.smartmall.tlai.web.order;

import com.smartmall.tlai.business.order.application.OrderService;
import com.smartmall.tlai.common.api.ApiResponse;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.web.order.dto.CreateOrderRequest;
import com.smartmall.tlai.web.order.dto.OrderResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('order:view')")
    public ApiResponse<PageResponse<OrderResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        var result = service.list(page, size, keyword, status);
        return ApiResponse.success(new PageResponse<>(result.items().stream()
                .map(OrderResponse::from).toList(), result.total(), result.page(), result.size()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order:view')")
    public ApiResponse<OrderResponse> get(@PathVariable @Min(1) long id) {
        return ApiResponse.success(OrderResponse.from(service.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('order:create')")
    public ResponseEntity<ApiResponse<OrderResponse>> create(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(OrderResponse.from(service.create(request.toCommand()))));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('order:pay')")
    public ApiResponse<OrderResponse> pay(@PathVariable @Min(1) long id) {
        return ApiResponse.success(OrderResponse.from(service.pay(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('order:cancel')")
    public ApiResponse<OrderResponse> cancel(@PathVariable @Min(1) long id) {
        return ApiResponse.success(OrderResponse.from(service.cancel(id)));
    }
}

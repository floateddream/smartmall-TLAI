package com.smartmall.tlai.web.customer;

import com.smartmall.tlai.business.customer.application.CustomerService;
import com.smartmall.tlai.common.api.ApiResponse;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.web.customer.dto.CustomerRequest;
import com.smartmall.tlai.web.customer.dto.CustomerResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService service;

    public CustomerController(CustomerService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('customer:view')")
    public ApiResponse<PageResponse<CustomerResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword) {
        var result = service.list(page, size, keyword);
        return ApiResponse.success(new PageResponse<>(result.items().stream()
                .map(CustomerResponse::from).toList(), result.total(), result.page(), result.size()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:view')")
    public ApiResponse<CustomerResponse> get(@PathVariable @Min(1) long id) {
        return ApiResponse.success(CustomerResponse.from(service.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('customer:manage')")
    public ResponseEntity<ApiResponse<CustomerResponse>> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(CustomerResponse.from(service.create(request.toCommand()))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:manage')")
    public ApiResponse<CustomerResponse> update(@PathVariable @Min(1) long id,
                                                @Valid @RequestBody CustomerRequest request) {
        return ApiResponse.success(CustomerResponse.from(service.update(id, request.toCommand())));
    }
}

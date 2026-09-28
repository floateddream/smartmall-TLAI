package com.smartmall.tlai.web.product;

import com.smartmall.tlai.business.product.application.ProductService;
import com.smartmall.tlai.common.api.ApiResponse;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.web.product.dto.ProductRequest;
import com.smartmall.tlai.web.product.dto.ProductResponse;
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
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('product:view')")
    public ApiResponse<PageResponse<ProductResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword) {
        var result = service.list(page, size, keyword);
        return ApiResponse.success(new PageResponse<>(result.items().stream()
                .map(ProductResponse::from).toList(), result.total(), result.page(), result.size()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:view')")
    public ApiResponse<ProductResponse> get(@PathVariable @Min(1) long id) {
        return ApiResponse.success(ProductResponse.from(service.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('product:manage')")
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ProductResponse.from(service.create(request.toCommand()))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:manage')")
    public ApiResponse<ProductResponse> update(@PathVariable @Min(1) long id,
                                               @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success(ProductResponse.from(service.update(id, request.toCommand())));
    }
}

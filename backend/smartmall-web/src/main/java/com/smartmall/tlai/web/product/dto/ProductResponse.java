package com.smartmall.tlai.web.product.dto;

import com.smartmall.tlai.business.product.domain.Product;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ProductResponse(Long id, String sku, String name, String description,
                              BigDecimal price, int stock, boolean active,
                              OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.id(), product.sku(), product.name(), product.description(),
                product.price(), product.stock(), product.active(), product.createdAt(), product.updatedAt());
    }
}

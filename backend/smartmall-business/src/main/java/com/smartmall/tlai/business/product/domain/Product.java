package com.smartmall.tlai.business.product.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 商品领域对象，不依赖 Controller 或 MyBatis。 */
public record Product(Long id, String sku, String name, String description,
                      BigDecimal price, int stock, boolean active,
                      OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}

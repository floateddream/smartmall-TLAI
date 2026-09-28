package com.smartmall.tlai.business.order.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record Order(Long id, String orderNo, Long customerId, String customerName,
                    BigDecimal totalAmount, OrderStatus status, List<OrderItem> items,
                    OffsetDateTime paidAt, OffsetDateTime cancelledAt, OffsetDateTime refundedAt,
                    OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public Order {
        items = items == null ? List.of() : List.copyOf(items);
    }
}

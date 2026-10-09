package com.smartmall.tlai.business.dashboard.model;

import com.smartmall.tlai.business.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record DashboardRecentOrder(
        long id,
        String orderNo,
        String productSummary,
        String customerName,
        BigDecimal totalAmount,
        OrderStatus status,
        OffsetDateTime createdAt
) {
}

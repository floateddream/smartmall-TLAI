package com.smartmall.tlai.web.dashboard.dto;

import com.smartmall.tlai.business.dashboard.model.DashboardRecentOrder;
import com.smartmall.tlai.business.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record DashboardRecentOrderResponse(long id, String orderNo, String productSummary, String customerName,
                                           BigDecimal totalAmount, OrderStatus status, OffsetDateTime createdAt) {
    public static DashboardRecentOrderResponse from(DashboardRecentOrder order) {
        return new DashboardRecentOrderResponse(order.id(), order.orderNo(), order.productSummary(),
                order.customerName(), order.totalAmount(), order.status(), order.createdAt());
    }
}

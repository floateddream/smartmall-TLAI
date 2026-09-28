package com.smartmall.tlai.web.order.dto;

import com.smartmall.tlai.business.order.domain.Order;
import com.smartmall.tlai.business.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponse(Long id, String orderNo, Long customerId, String customerName,
                            BigDecimal totalAmount, OrderStatus status, List<OrderItemResponse> items,
                            OffsetDateTime paidAt, OffsetDateTime cancelledAt, OffsetDateTime refundedAt,
                            OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(order.id(), order.orderNo(), order.customerId(), order.customerName(),
                order.totalAmount(), order.status(), order.items().stream().map(OrderItemResponse::from).toList(),
                order.paidAt(), order.cancelledAt(), order.refundedAt(), order.createdAt(), order.updatedAt());
    }
}

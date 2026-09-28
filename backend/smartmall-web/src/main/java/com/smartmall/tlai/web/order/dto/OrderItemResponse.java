package com.smartmall.tlai.web.order.dto;

import com.smartmall.tlai.business.order.domain.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(Long id, Long productId, String productSku, String productName,
                                BigDecimal unitPrice, int quantity, BigDecimal subtotal) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.id(), item.productId(), item.productSku(), item.productName(),
                item.unitPrice(), item.quantity(), item.subtotal());
    }
}

package com.smartmall.tlai.business.order.domain;

/** 订单状态只允许沿着业务服务定义的路径变化。 */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    CANCELLED,
    REFUNDING,
    REFUNDED
}

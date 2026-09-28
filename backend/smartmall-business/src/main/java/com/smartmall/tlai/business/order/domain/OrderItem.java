package com.smartmall.tlai.business.order.domain;

import java.math.BigDecimal;

/** 商品名称、SKU 和价格是下单时的快照，后续修改商品不会改写历史订单。 */
public record OrderItem(Long id, Long productId, String productSku, String productName,
                        BigDecimal unitPrice, int quantity, BigDecimal subtotal) {
}

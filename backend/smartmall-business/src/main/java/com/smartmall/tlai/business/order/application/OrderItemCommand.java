package com.smartmall.tlai.business.order.application;

/** 创建订单时前端只提交商品 ID 和数量，价格必须由后端读取。 */
public record OrderItemCommand(long productId, int quantity) {
}

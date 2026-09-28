package com.smartmall.tlai.business.order.application;

import java.util.List;

public record CreateOrderCommand(long customerId, List<OrderItemCommand> items) {
}

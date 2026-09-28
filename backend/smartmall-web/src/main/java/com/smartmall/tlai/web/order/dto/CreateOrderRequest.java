package com.smartmall.tlai.web.order.dto;

import com.smartmall.tlai.business.order.application.CreateOrderCommand;
import com.smartmall.tlai.business.order.application.OrderItemCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotNull @Positive Long customerId,
        @NotEmpty @Size(max = 100) List<@Valid OrderItemRequest> items) {
    public CreateOrderCommand toCommand() {
        return new CreateOrderCommand(customerId,
                items.stream().map(item -> new OrderItemCommand(item.productId(), item.quantity())).toList());
    }
}

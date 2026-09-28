package com.smartmall.tlai.web.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(
        @NotNull @Positive Long productId,
        @NotNull @Positive @Max(999999) Integer quantity) {
}

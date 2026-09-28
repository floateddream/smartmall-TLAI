package com.smartmall.tlai.web.customer.dto;

import com.smartmall.tlai.business.customer.domain.Customer;
import java.time.OffsetDateTime;

public record CustomerResponse(Long id, String name, String phone, String email,
                               OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.id(), customer.name(), customer.phone(), customer.email(),
                customer.createdAt(), customer.updatedAt());
    }
}

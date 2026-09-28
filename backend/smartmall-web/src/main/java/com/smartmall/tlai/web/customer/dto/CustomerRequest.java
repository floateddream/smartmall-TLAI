package com.smartmall.tlai.web.customer.dto;

import com.smartmall.tlai.business.customer.application.CustomerCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 32) String phone,
        @Email @Size(max = 128) String email) {
    public CustomerCommand toCommand() { return new CustomerCommand(name, phone, email); }
}

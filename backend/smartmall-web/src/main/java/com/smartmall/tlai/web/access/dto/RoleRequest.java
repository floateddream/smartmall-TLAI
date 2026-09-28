package com.smartmall.tlai.web.access.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RoleRequest(
        @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,63}$") String code,
        @NotBlank @Size(max = 64) String name,
        @Size(max = 300) String description,
        List<@Pattern(regexp = "^[a-z][a-z0-9]*:[a-z][a-z0-9_]*$") String> permissionCodes) {
}

package com.smartmall.tlai.web.access.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(@NotBlank @Size(max = 64) String name,
                                @Size(max = 300) String description) {
}

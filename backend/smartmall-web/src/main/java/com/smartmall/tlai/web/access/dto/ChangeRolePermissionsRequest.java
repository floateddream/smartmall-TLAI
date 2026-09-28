package com.smartmall.tlai.web.access.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record ChangeRolePermissionsRequest(
        @NotNull List<@Pattern(regexp = "^[a-z][a-z0-9]*:[a-z][a-z0-9_]*$") String> permissionCodes) {
}

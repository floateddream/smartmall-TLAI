package com.smartmall.tlai.web.access.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record ChangeUserRolesRequest(
        @NotEmpty List<@Pattern(regexp = "^[A-Z][A-Z0-9_]{1,63}$") String> roleCodes) {
}

package com.smartmall.tlai.web.access.dto;

import com.smartmall.tlai.business.user.domain.UserValidationRules;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateAdminUserRequest(
        @NotBlank @Pattern(regexp = UserValidationRules.USERNAME_PATTERN) String username,
        @NotBlank @Email @Size(max = 128) String email,
        @NotBlank @Pattern(regexp = UserValidationRules.PASSWORD_PATTERN) String password,
        @NotEmpty List<@Pattern(regexp = "^[A-Z][A-Z0-9_]{1,63}$") String> roleCodes) {
}

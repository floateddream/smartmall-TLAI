package com.smartmall.tlai.web.auth.dto;

import com.smartmall.tlai.business.user.domain.UserValidationRules;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = UserValidationRules.USERNAME_PATTERN,
                message = "用户名必须为 4-32 位字母、数字或下划线") String username,
        @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确")
        @Size(max = 128, message = "邮箱长度不能超过 128 位") String email,
        @NotBlank(message = "密码不能为空")
        @Pattern(regexp = UserValidationRules.PASSWORD_PATTERN,
                message = "密码必须为 8-64 位，并且至少包含一个字母和一个数字") String password) {
}

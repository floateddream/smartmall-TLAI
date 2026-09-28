package com.smartmall.tlai.web.access.dto;

import com.smartmall.tlai.business.user.domain.UserAccount;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminUserResponse(Long id, String username, String email, boolean enabled,
                                List<String> roles, List<String> permissions,
                                OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static AdminUserResponse from(UserAccount user) {
        return new AdminUserResponse(user.id(), user.username(), user.email(), user.enabled(),
                user.roleCodes(), user.permissions(), user.createdAt(), user.updatedAt());
    }
}

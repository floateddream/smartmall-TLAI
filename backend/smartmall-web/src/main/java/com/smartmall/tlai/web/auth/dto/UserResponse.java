package com.smartmall.tlai.web.auth.dto;

import com.smartmall.tlai.security.auth.AuthenticatedUser;

import java.util.List;

public record UserResponse(Long id, String username, String email,
                           List<String> roles, List<String> permissions, boolean enabled) {
    public static UserResponse from(AuthenticatedUser user) {
        return new UserResponse(user.id(), user.username(), user.email(),
                user.roles(), user.permissions(), user.enabled());
    }
}

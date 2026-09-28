package com.smartmall.tlai.web.auth.dto;

import com.smartmall.tlai.security.auth.AuthResult;

public record AuthResponse(String tokenType, String accessToken, long expiresIn, UserResponse user) {
    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(result.tokenType(), result.accessToken(), result.expiresIn(),
                UserResponse.from(result.user()));
    }
}

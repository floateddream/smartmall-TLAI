package com.smartmall.tlai.security.auth;

public record AuthResult(String tokenType, String accessToken, long expiresIn, AuthenticatedUser user) {
}

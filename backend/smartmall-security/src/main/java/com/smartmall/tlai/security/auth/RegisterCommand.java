package com.smartmall.tlai.security.auth;

public record RegisterCommand(String username, String email, String password) {
}

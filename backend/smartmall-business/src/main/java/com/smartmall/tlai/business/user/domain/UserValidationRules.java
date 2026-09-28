package com.smartmall.tlai.business.user.domain;

public final class UserValidationRules {
    public static final String USERNAME_PATTERN = "^[A-Za-z0-9_]{4,32}$";
    public static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";

    private UserValidationRules() {
    }
}

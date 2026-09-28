package com.smartmall.tlai.business.user.application;

public record ChangeUserEnabledCommand(boolean enabled, long operatorId, String operatorName) {
}

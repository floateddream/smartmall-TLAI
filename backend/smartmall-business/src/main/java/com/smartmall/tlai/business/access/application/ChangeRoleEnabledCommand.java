package com.smartmall.tlai.business.access.application;

public record ChangeRoleEnabledCommand(boolean enabled, long operatorId, String operatorName) {
}

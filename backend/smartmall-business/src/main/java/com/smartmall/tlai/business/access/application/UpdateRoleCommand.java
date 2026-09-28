package com.smartmall.tlai.business.access.application;

public record UpdateRoleCommand(String name, String description, long operatorId, String operatorName) {
}

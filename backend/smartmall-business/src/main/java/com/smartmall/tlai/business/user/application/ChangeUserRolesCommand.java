package com.smartmall.tlai.business.user.application;

import java.util.List;

public record ChangeUserRolesCommand(List<String> roleCodes, long operatorId, String operatorName) {
}

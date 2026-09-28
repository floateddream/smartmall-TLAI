package com.smartmall.tlai.business.access.application;

import java.util.List;

public record ChangeRolePermissionsCommand(List<String> permissionCodes,
                                           long operatorId, String operatorName) {
}

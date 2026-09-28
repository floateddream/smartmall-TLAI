package com.smartmall.tlai.business.access.application;

import java.util.List;

public record RoleCommand(String code, String name, String description, List<String> permissionCodes,
                          long operatorId, String operatorName) {
}

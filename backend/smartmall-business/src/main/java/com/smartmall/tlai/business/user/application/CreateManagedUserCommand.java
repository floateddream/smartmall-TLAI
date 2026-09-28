package com.smartmall.tlai.business.user.application;

import java.util.List;

public record CreateManagedUserCommand(String username, String email, String rawPassword,
                                       List<String> roleCodes, long operatorId, String operatorName) {
}

package com.smartmall.tlai.web.access.dto;

import com.smartmall.tlai.business.access.domain.PermissionDefinition;

public record PermissionResponse(Long id, String code, String name, String module, String description) {
    public static PermissionResponse from(PermissionDefinition permission) {
        return new PermissionResponse(permission.id(), permission.code(), permission.name(),
                permission.module(), permission.description());
    }
}

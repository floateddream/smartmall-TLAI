package com.smartmall.tlai.web.access.dto;

import com.smartmall.tlai.business.access.domain.AccessRole;

import java.time.OffsetDateTime;
import java.util.List;

public record RoleResponse(Long id, String code, String name, String description,
                           boolean builtIn, boolean enabled, List<String> permissions,
                           OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static RoleResponse from(AccessRole role) {
        return new RoleResponse(role.id(), role.code(), role.name(), role.description(),
                role.builtIn(), role.enabled(), role.permissions(), role.createdAt(), role.updatedAt());
    }
}

package com.smartmall.tlai.business.access.domain;

import java.time.OffsetDateTime;
import java.util.List;

public record AccessRole(Long id, String code, String name, String description,
                         boolean builtIn, boolean enabled, List<String> permissions,
                         OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public AccessRole {
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }
}

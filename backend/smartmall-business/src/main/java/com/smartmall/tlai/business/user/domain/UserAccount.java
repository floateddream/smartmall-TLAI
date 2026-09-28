package com.smartmall.tlai.business.user.domain;

import java.time.OffsetDateTime;
import java.util.List;

/** 后台登录账户领域对象；passwordHash 永远不能直接返回到接口。 */
public record UserAccount(Long id, String username, String email, String passwordHash, boolean enabled,
                          List<String> roleCodes, List<String> permissions,
                          OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public UserAccount {
        roleCodes = roleCodes == null ? List.of() : List.copyOf(roleCodes);
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }

    public boolean hasRole(String roleCode) {
        return roleCodes.contains(roleCode);
    }

    public boolean hasPermission(String permissionCode) {
        return permissions.contains(permissionCode);
    }
}

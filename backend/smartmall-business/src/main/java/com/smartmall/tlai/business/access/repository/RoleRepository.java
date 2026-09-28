package com.smartmall.tlai.business.access.repository;

import com.smartmall.tlai.business.access.domain.AccessRole;
import com.smartmall.tlai.business.access.domain.PermissionDefinition;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface RoleRepository {
    List<AccessRole> findAll();
    Optional<AccessRole> findById(long id);
    Optional<AccessRole> findByCode(String code);
    List<String> findEnabledCodes(List<String> codes);
    List<String> findExistingPermissionCodes(List<String> codes);
    List<PermissionDefinition> findAllPermissions();
    AccessRole insert(AccessRole role);
    AccessRole update(AccessRole role);
    void replacePermissions(long roleId, List<String> permissionCodes);
    boolean updateEnabled(long roleId, boolean enabled, OffsetDateTime changedAt);
}

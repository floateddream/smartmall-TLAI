package com.smartmall.tlai.business.access.application;

import com.smartmall.tlai.business.access.domain.AccessRole;
import com.smartmall.tlai.business.access.domain.PermissionAudit;
import com.smartmall.tlai.business.access.domain.PermissionDefinition;
import com.smartmall.tlai.business.access.repository.PermissionAuditRepository;
import com.smartmall.tlai.business.access.repository.RoleRepository;
import com.smartmall.tlai.business.user.domain.BuiltInRoleCodes;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AccessControlService {
    private final RoleRepository roleRepository;
    private final PermissionAuditRepository auditRepository;

    public AccessControlService(RoleRepository roleRepository, PermissionAuditRepository auditRepository) {
        this.roleRepository = roleRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional(readOnly = true)
    public List<AccessRole> listRoles() {
        return roleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<PermissionDefinition> listPermissions() {
        return roleRepository.findAllPermissions();
    }

    @Transactional
    public AccessRole createRole(RoleCommand command) {
        String code = normalizeRoleCode(command.code());
        if (roleRepository.findByCode(code).isPresent()) {
            throw new BusinessException(ErrorCode.ROLE_CODE_EXISTS);
        }
        List<String> permissionCodes = requirePermissions(command.permissionCodes());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        AccessRole inserted = roleRepository.insert(new AccessRole(null, code, command.name().trim(),
                trimToNull(command.description()), false, true, List.of(), now, now));
        roleRepository.replacePermissions(inserted.id(), permissionCodes);
        audit(command.operatorId(), command.operatorName(), inserted, "ROLE_CREATED", null,
                "permissions=" + String.join(",", permissionCodes) + ";enabled=true", now);
        return requireRole(inserted.id());
    }

    @Transactional
    public AccessRole updateRole(long roleId, UpdateRoleCommand command) {
        AccessRole current = requireRole(roleId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        AccessRole updated = roleRepository.update(new AccessRole(current.id(), current.code(),
                command.name().trim(), trimToNull(command.description()), current.builtIn(), current.enabled(),
                current.permissions(), current.createdAt(), now));
        audit(command.operatorId(), command.operatorName(), updated, "ROLE_UPDATED",
                current.name() + "|" + nullToEmpty(current.description()),
                updated.name() + "|" + nullToEmpty(updated.description()), now);
        return requireRole(roleId);
    }

    @Transactional
    public AccessRole changePermissions(long roleId, ChangeRolePermissionsCommand command) {
        AccessRole role = requireRole(roleId);
        if (BuiltInRoleCodes.ADMIN.equals(role.code())) {
            throw new BusinessException(ErrorCode.BUILT_IN_ROLE_PROTECTED, "内置管理员必须保留全部权限");
        }
        List<String> permissionCodes = requirePermissions(command.permissionCodes());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        roleRepository.replacePermissions(roleId, permissionCodes);
        audit(command.operatorId(), command.operatorName(), role, "ROLE_PERMISSIONS_CHANGED",
                String.join(",", role.permissions()), String.join(",", permissionCodes), now);
        return requireRole(roleId);
    }

    @Transactional
    public AccessRole changeEnabled(long roleId, ChangeRoleEnabledCommand command) {
        AccessRole role = requireRole(roleId);
        if (!command.enabled() && BuiltInRoleCodes.ADMIN.equals(role.code())) {
            throw new BusinessException(ErrorCode.BUILT_IN_ROLE_PROTECTED, "内置管理员不能停用");
        }
        if (role.enabled() != command.enabled()) {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            roleRepository.updateEnabled(roleId, command.enabled(), now);
            audit(command.operatorId(), command.operatorName(), role, "ROLE_ENABLED_CHANGED",
                    Boolean.toString(role.enabled()), Boolean.toString(command.enabled()), now);
        }
        return requireRole(roleId);
    }

    @Transactional(readOnly = true)
    public PageResponse<PermissionAudit> listAudits(int page, int size, String targetType, Long targetId) {
        String normalizedType = targetType == null || targetType.isBlank()
                ? null : targetType.trim().toUpperCase(Locale.ROOT);
        return new PageResponse<>(auditRepository.findPage(page, size, normalizedType, targetId),
                auditRepository.count(normalizedType, targetId), page, size);
    }

    private AccessRole requireRole(long roleId) {
        return roleRepository.findById(roleId).orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
    }

    private List<String> requirePermissions(List<String> requestedCodes) {
        Set<String> normalized = new LinkedHashSet<>();
        if (requestedCodes != null) {
            requestedCodes.stream()
                    .filter(code -> code != null && !code.isBlank())
                    .map(String::trim)
                    .forEach(normalized::add);
        }
        List<String> existingCodes = roleRepository.findExistingPermissionCodes(List.copyOf(normalized));
        if (existingCodes.size() != normalized.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "包含不存在的权限编码");
        }
        return List.copyOf(normalized);
    }

    private static String normalizeRoleCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void audit(long operatorId, String operatorName, AccessRole role, String action,
                       String beforeValue, String afterValue, OffsetDateTime createdAt) {
        auditRepository.insert(new PermissionAudit(null, operatorId, operatorName,
                "ROLE", role.id(), role.name(), action, beforeValue, afterValue, createdAt));
    }
}

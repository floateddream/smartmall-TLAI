package com.smartmall.tlai.infrastructure.persistence.access;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartmall.tlai.business.access.domain.AccessRole;
import com.smartmall.tlai.business.access.domain.PermissionDefinition;
import com.smartmall.tlai.business.access.repository.RoleRepository;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class MybatisRoleRepository implements RoleRepository {
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    public MybatisRoleRepository(RoleMapper roleMapper, PermissionMapper permissionMapper,
                                 RolePermissionMapper rolePermissionMapper) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    @Override
    public List<AccessRole> findAll() {
        return aggregate(roleMapper.selectAllWithPermissions());
    }

    @Override
    public Optional<AccessRole> findById(long id) {
        return aggregateOne(roleMapper.selectByIdWithPermissions(id));
    }

    @Override
    public Optional<AccessRole> findByCode(String code) {
        return aggregateOne(roleMapper.selectByCodeWithPermissions(code));
    }

    @Override
    public List<String> findEnabledCodes(List<String> codes) {
        return codes.isEmpty() ? List.of() : roleMapper.selectEnabledCodes(codes);
    }

    @Override
    public List<String> findExistingPermissionCodes(List<String> codes) {
        return codes.isEmpty() ? List.of() : permissionMapper.selectExistingCodes(codes);
    }

    @Override
    public List<PermissionDefinition> findAllPermissions() {
        return permissionMapper.selectList(Wrappers.<PermissionPersistenceEntity>lambdaQuery()
                        .orderByAsc(PermissionPersistenceEntity::getModule)
                        .orderByAsc(PermissionPersistenceEntity::getId))
                .stream().map(this::toPermission).toList();
    }

    @Override
    public AccessRole insert(AccessRole role) {
        RolePersistenceEntity entity = toEntity(role);
        try {
            roleMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.ROLE_CODE_EXISTS);
        }
        return toDomain(entity, List.of());
    }

    @Override
    public AccessRole update(AccessRole role) {
        roleMapper.updateDefinition(role.id(), role.name(), role.description(), role.updatedAt());
        return role;
    }

    @Override
    public void replacePermissions(long roleId, List<String> permissionCodes) {
        rolePermissionMapper.deleteByRoleId(roleId);
        if (!permissionCodes.isEmpty()) {
            rolePermissionMapper.insertPermissions(roleId, permissionCodes);
        }
    }

    @Override
    public boolean updateEnabled(long roleId, boolean enabled, OffsetDateTime changedAt) {
        return roleMapper.updateEnabled(roleId, enabled, changedAt) == 1;
    }

    private Optional<AccessRole> aggregateOne(List<RoleAccessRow> rows) {
        List<AccessRole> roles = aggregate(rows);
        return roles.isEmpty() ? Optional.empty() : Optional.of(roles.getFirst());
    }

    private List<AccessRole> aggregate(List<RoleAccessRow> rows) {
        Map<Long, RoleAggregate> roles = new LinkedHashMap<>();
        for (RoleAccessRow row : rows) {
            RoleAggregate aggregate = roles.computeIfAbsent(row.getId(), ignored -> new RoleAggregate(row));
            if (row.getPermissionCode() != null) aggregate.permissions.add(row.getPermissionCode());
        }
        List<AccessRole> result = new ArrayList<>(roles.size());
        roles.values().forEach(aggregate -> result.add(aggregate.toDomain()));
        return result;
    }

    private AccessRole toDomain(RolePersistenceEntity entity, List<String> permissions) {
        return new AccessRole(entity.getId(), entity.getCode(), entity.getName(), entity.getDescription(),
                Boolean.TRUE.equals(entity.getBuiltIn()), Boolean.TRUE.equals(entity.getEnabled()), permissions,
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private PermissionDefinition toPermission(PermissionPersistenceEntity entity) {
        return new PermissionDefinition(entity.getId(), entity.getCode(), entity.getName(),
                entity.getModule(), entity.getDescription());
    }

    private RolePersistenceEntity toEntity(AccessRole role) {
        RolePersistenceEntity entity = new RolePersistenceEntity();
        entity.setId(role.id());
        entity.setCode(role.code());
        entity.setName(role.name());
        entity.setDescription(role.description());
        entity.setBuiltIn(role.builtIn());
        entity.setEnabled(role.enabled());
        entity.setCreatedAt(role.createdAt());
        entity.setUpdatedAt(role.updatedAt());
        return entity;
    }

    private static final class RoleAggregate {
        private final RoleAccessRow row;
        private final LinkedHashSet<String> permissions = new LinkedHashSet<>();

        private RoleAggregate(RoleAccessRow row) {
            this.row = row;
        }

        private AccessRole toDomain() {
            return new AccessRole(row.getId(), row.getCode(), row.getName(), row.getDescription(),
                    Boolean.TRUE.equals(row.getBuiltIn()), Boolean.TRUE.equals(row.getEnabled()),
                    List.copyOf(permissions), row.getCreatedAt(), row.getUpdatedAt());
        }
    }
}

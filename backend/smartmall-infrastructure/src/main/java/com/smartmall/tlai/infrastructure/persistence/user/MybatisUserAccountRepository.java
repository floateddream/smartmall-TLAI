package com.smartmall.tlai.infrastructure.persistence.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartmall.tlai.business.user.domain.UserAccount;
import com.smartmall.tlai.business.user.repository.UserAccountRepository;
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
public class MybatisUserAccountRepository implements UserAccountRepository {
    private final UserMapper mapper;
    private final UserRoleMapper userRoleMapper;

    public MybatisUserAccountRepository(UserMapper mapper, UserRoleMapper userRoleMapper) {
        this.mapper = mapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public Optional<UserAccount> findByLogin(String account, String normalizedEmail) {
        return aggregateOne(mapper.selectByLoginWithAccess(account, normalizedEmail));
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        UserPersistenceEntity user = mapper.selectOne(Wrappers.<UserPersistenceEntity>lambdaQuery()
                .eq(UserPersistenceEntity::getUsername, username).last("LIMIT 1"));
        return user == null ? Optional.empty() : findById(user.getId());
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        UserPersistenceEntity user = mapper.selectOne(Wrappers.<UserPersistenceEntity>lambdaQuery()
                .eq(UserPersistenceEntity::getEmail, email).last("LIMIT 1"));
        return user == null ? Optional.empty() : findById(user.getId());
    }

    @Override
    public Optional<UserAccount> findById(long id) {
        return aggregateOne(mapper.selectByIdWithAccess(id));
    }

    @Override
    public long count(String keyword, String roleCode, Boolean enabled) {
        return mapper.countUsers(keyword, roleCode, enabled);
    }

    @Override
    public List<UserAccount> findPage(int page, int size, String keyword, String roleCode, Boolean enabled) {
        long offset = ((long) page - 1) * size;
        List<Long> ids = mapper.selectPageIds(offset, size, keyword, roleCode, enabled);
        if (ids.isEmpty()) return List.of();
        return aggregate(mapper.selectByIdsWithAccess(ids));
    }

    @Override
    public boolean hasRole(String roleCode) {
        return mapper.hasRole(roleCode);
    }

    @Override
    public long countEnabledUsersWithRole(String roleCode) {
        return mapper.countEnabledUsersWithRole(roleCode);
    }

    @Override
    public UserAccount insert(UserAccount user) {
        UserPersistenceEntity entity = toEntity(user);
        try {
            mapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            if (containsConstraintName(exception, "uk_app_user_email")) {
                throw new BusinessException(ErrorCode.EMAIL_EXISTS);
            }
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        return toDomain(entity, List.of(), List.of());
    }

    @Override
    public void replaceRoles(long userId, List<String> roleCodes, Long assignedBy, OffsetDateTime changedAt) {
        userRoleMapper.deleteByUserId(userId);
        userRoleMapper.insertRoles(userId, roleCodes, assignedBy, changedAt);
    }

    @Override
    public boolean updateEnabled(long userId, boolean enabled, OffsetDateTime changedAt) {
        return mapper.updateEnabled(userId, enabled, changedAt) == 1;
    }

    @Override
    public void lockAccessControl() {
        mapper.lockAccessControl();
    }

    private Optional<UserAccount> aggregateOne(List<UserAccessRow> rows) {
        List<UserAccount> users = aggregate(rows);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.getFirst());
    }

    /** 联查结果会因角色和权限展开成多行，这里按用户去重聚合。 */
    private List<UserAccount> aggregate(List<UserAccessRow> rows) {
        Map<Long, UserAggregate> users = new LinkedHashMap<>();
        for (UserAccessRow row : rows) {
            UserAggregate aggregate = users.computeIfAbsent(row.getId(), ignored -> new UserAggregate(row));
            if (row.getRoleCode() != null) aggregate.roles.add(row.getRoleCode());
            if (row.getPermissionCode() != null) aggregate.permissions.add(row.getPermissionCode());
        }
        List<UserAccount> result = new ArrayList<>(users.size());
        users.values().forEach(aggregate -> result.add(aggregate.toDomain()));
        return result;
    }

    private UserAccount toDomain(UserPersistenceEntity entity, List<String> roles, List<String> permissions) {
        return new UserAccount(entity.getId(), entity.getUsername(), entity.getEmail(), entity.getPasswordHash(),
                Boolean.TRUE.equals(entity.getEnabled()), roles, permissions,
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private UserPersistenceEntity toEntity(UserAccount user) {
        UserPersistenceEntity entity = new UserPersistenceEntity();
        entity.setId(user.id());
        entity.setUsername(user.username());
        entity.setEmail(user.email());
        entity.setPasswordHash(user.passwordHash());
        entity.setEnabled(user.enabled());
        entity.setCreatedAt(user.createdAt());
        entity.setUpdatedAt(user.updatedAt());
        return entity;
    }

    private boolean containsConstraintName(Throwable throwable, String constraintName) {
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains(constraintName)) return true;
            current = current.getCause();
        }
        return false;
    }

    private static final class UserAggregate {
        private final UserAccessRow row;
        private final LinkedHashSet<String> roles = new LinkedHashSet<>();
        private final LinkedHashSet<String> permissions = new LinkedHashSet<>();

        private UserAggregate(UserAccessRow row) {
            this.row = row;
        }

        private UserAccount toDomain() {
            return new UserAccount(row.getId(), row.getUsername(), row.getEmail(), row.getPasswordHash(),
                    Boolean.TRUE.equals(row.getEnabled()), List.copyOf(roles), List.copyOf(permissions),
                    row.getCreatedAt(), row.getUpdatedAt());
        }
    }
}

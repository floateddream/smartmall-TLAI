package com.smartmall.tlai.business.user.application;

import com.smartmall.tlai.business.access.domain.PermissionAudit;
import com.smartmall.tlai.business.access.repository.PermissionAuditRepository;
import com.smartmall.tlai.business.access.repository.RoleRepository;
import com.smartmall.tlai.business.user.domain.BuiltInRoleCodes;
import com.smartmall.tlai.business.user.domain.UserAccount;
import com.smartmall.tlai.business.user.repository.UserAccountRepository;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class UserAccountService {
    private final UserAccountRepository repository;
    private final RoleRepository roleRepository;
    private final PermissionAuditRepository auditRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountRepository repository, RoleRepository roleRepository,
                              PermissionAuditRepository auditRepository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.roleRepository = roleRepository;
        this.auditRepository = auditRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount createUser(String username, String email, String rawPassword, String roleCode) {
        return createUser(username, email, rawPassword, List.of(roleCode), null, null, false);
    }

    @Transactional
    public UserAccount createManagedUser(CreateManagedUserCommand command) {
        return createUser(command.username(), command.email(), command.rawPassword(), command.roleCodes(),
                command.operatorId(), command.operatorName(), true);
    }

    private UserAccount createUser(String username, String email, String rawPassword, List<String> roleCodes,
                                   Long operatorId, String operatorName, boolean audited) {
        String normalizedEmail = normalizeEmail(email);
        if (repository.findByUsername(username).isPresent()) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        if (repository.findByEmail(normalizedEmail).isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }

        List<String> validRoleCodes = requireEnabledRoles(roleCodes);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        UserAccount user = new UserAccount(null, username, normalizedEmail,
                passwordEncoder.encode(rawPassword), true, List.of(), List.of(), now, now);
        UserAccount inserted = repository.insert(user);
        repository.replaceRoles(inserted.id(), validRoleCodes, operatorId, now);

        if (audited) {
            audit(operatorId, operatorName, "USER", inserted.id(), inserted.username(),
                    "USER_CREATED", null, "roles=" + String.join(",", validRoleCodes) + ";enabled=true", now);
        }
        return repository.findById(inserted.id()).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> findByLogin(String account) {
        String normalizedAccount = account.trim();
        return repository.findByLogin(normalizedAccount, normalizeEmail(normalizedAccount));
    }

    @Transactional(readOnly = true)
    public boolean hasAdmin() {
        return repository.hasRole(BuiltInRoleCodes.ADMIN);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserAccount> list(int page, int size, String keyword, String roleCode, Boolean enabled) {
        String search = keyword == null ? null : keyword.trim();
        String normalizedRole = roleCode == null || roleCode.isBlank()
                ? null : roleCode.trim().toUpperCase(Locale.ROOT);
        return new PageResponse<>(repository.findPage(page, size, search, normalizedRole, enabled),
                repository.count(search, normalizedRole, enabled), page, size);
    }

    @Transactional
    public UserAccount changeRoles(long userId, ChangeUserRolesCommand command) {
        ensureNotSelf(userId, command.operatorId());
        repository.lockAccessControl();
        UserAccount user = requireUser(userId);
        List<String> roleCodes = requireEnabledRoles(command.roleCodes());

        if (user.enabled() && user.hasRole(BuiltInRoleCodes.ADMIN)
                && !roleCodes.contains(BuiltInRoleCodes.ADMIN)
                && repository.countEnabledUsersWithRole(BuiltInRoleCodes.ADMIN) <= 1) {
            throw new BusinessException(ErrorCode.LAST_ADMIN_REQUIRED);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        repository.replaceRoles(userId, roleCodes, command.operatorId(), now);
        audit(command.operatorId(), command.operatorName(), "USER", user.id(), user.username(),
                "USER_ROLES_CHANGED", String.join(",", user.roleCodes()), String.join(",", roleCodes), now);
        return requireUser(userId);
    }

    @Transactional
    public UserAccount changeEnabled(long userId, ChangeUserEnabledCommand command) {
        ensureNotSelf(userId, command.operatorId());
        repository.lockAccessControl();
        UserAccount user = requireUser(userId);

        if (!command.enabled() && user.enabled() && user.hasRole(BuiltInRoleCodes.ADMIN)
                && repository.countEnabledUsersWithRole(BuiltInRoleCodes.ADMIN) <= 1) {
            throw new BusinessException(ErrorCode.LAST_ADMIN_REQUIRED);
        }

        if (user.enabled() != command.enabled()) {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            repository.updateEnabled(userId, command.enabled(), now);
            audit(command.operatorId(), command.operatorName(), "USER", user.id(), user.username(),
                    "USER_ENABLED_CHANGED", Boolean.toString(user.enabled()),
                    Boolean.toString(command.enabled()), now);
        }
        return requireUser(userId);
    }

    private UserAccount requireUser(long userId) {
        return repository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private List<String> requireEnabledRoles(List<String> requestedCodes) {
        Set<String> normalized = new LinkedHashSet<>();
        if (requestedCodes != null) {
            requestedCodes.stream()
                    .filter(code -> code != null && !code.isBlank())
                    .map(code -> code.trim().toUpperCase(Locale.ROOT))
                    .forEach(normalized::add);
        }
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.ROLE_ASSIGNMENT_REQUIRED);
        }

        List<String> enabledCodes = roleRepository.findEnabledCodes(List.copyOf(normalized));
        if (enabledCodes.size() != normalized.size()) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND, "包含不存在或已停用的角色");
        }
        return List.copyOf(normalized);
    }

    private static void ensureNotSelf(long userId, long operatorId) {
        if (userId == operatorId) {
            throw new BusinessException(ErrorCode.SELF_PERMISSION_CHANGE_FORBIDDEN);
        }
    }

    private void audit(Long operatorId, String operatorName, String targetType, long targetId,
                       String targetName, String action, String beforeValue, String afterValue,
                       OffsetDateTime createdAt) {
        auditRepository.insert(new PermissionAudit(null, operatorId, operatorName,
                targetType, targetId, targetName, action, beforeValue, afterValue, createdAt));
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

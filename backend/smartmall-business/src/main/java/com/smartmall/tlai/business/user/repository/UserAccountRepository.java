package com.smartmall.tlai.business.user.repository;

import com.smartmall.tlai.business.user.domain.UserAccount;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UserAccountRepository {
    Optional<UserAccount> findByLogin(String account, String normalizedEmail);
    Optional<UserAccount> findByUsername(String username);
    Optional<UserAccount> findByEmail(String email);
    Optional<UserAccount> findById(long id);
    long count(String keyword, String roleCode, Boolean enabled);
    List<UserAccount> findPage(int page, int size, String keyword, String roleCode, Boolean enabled);
    boolean hasRole(String roleCode);
    long countEnabledUsersWithRole(String roleCode);
    UserAccount insert(UserAccount user);
    void replaceRoles(long userId, List<String> roleCodes, Long assignedBy, OffsetDateTime changedAt);
    boolean updateEnabled(long userId, boolean enabled, OffsetDateTime changedAt);
    void lockAccessControl();
}

package com.smartmall.tlai.business.user.application;

import com.smartmall.tlai.business.access.repository.PermissionAuditRepository;
import com.smartmall.tlai.business.access.repository.RoleRepository;
import com.smartmall.tlai.business.user.domain.BuiltInRoleCodes;
import com.smartmall.tlai.business.user.domain.UserAccount;
import com.smartmall.tlai.business.user.repository.UserAccountRepository;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {
    @Mock UserAccountRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PermissionAuditRepository auditRepository;
    @Mock PasswordEncoder passwordEncoder;
    private UserAccountService service;

    @BeforeEach
    void setUp() {
        service = new UserAccountService(userRepository, roleRepository, auditRepository, passwordEncoder);
    }

    @Test
    void shouldRejectChangingOwnRoles() {
        var command = new ChangeUserRolesCommand(List.of(BuiltInRoleCodes.OPERATOR), 7L, "operator");

        assertThatThrownBy(() -> service.changeRoles(7L, command))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ErrorCode.SELF_PERMISSION_CHANGE_FORBIDDEN));
    }

    @Test
    void shouldKeepAtLeastOneEnabledAdmin() {
        UserAccount admin = user(2L, true, BuiltInRoleCodes.ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(roleRepository.findEnabledCodes(List.of(BuiltInRoleCodes.OPERATOR)))
                .thenReturn(List.of(BuiltInRoleCodes.OPERATOR));
        when(userRepository.countEnabledUsersWithRole(BuiltInRoleCodes.ADMIN)).thenReturn(1L);

        var command = new ChangeUserRolesCommand(List.of(BuiltInRoleCodes.OPERATOR), 1L, "another_admin");

        assertThatThrownBy(() -> service.changeRoles(2L, command))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.LAST_ADMIN_REQUIRED));
        verify(userRepository).lockAccessControl();
    }

    @Test
    void shouldDisableOrdinaryUserAndWriteAudit() {
        UserAccount operator = user(3L, true, BuiltInRoleCodes.OPERATOR);
        UserAccount disabled = user(3L, false, BuiltInRoleCodes.OPERATOR);
        when(userRepository.findById(3L)).thenReturn(Optional.of(operator), Optional.of(disabled));
        when(userRepository.updateEnabled(3L, false, org.mockito.ArgumentMatchers.any())).thenReturn(true);

        UserAccount result = service.changeEnabled(3L,
                new ChangeUserEnabledCommand(false, 1L, "admin"));

        assertThat(result.enabled()).isFalse();
        verify(auditRepository).insert(org.mockito.ArgumentMatchers.any());
    }

    private UserAccount user(long id, boolean enabled, String roleCode) {
        OffsetDateTime now = OffsetDateTime.parse("2030-01-01T00:00:00Z");
        return new UserAccount(id, "user_" + id, "user" + id + "@example.com", "hash", enabled,
                List.of(roleCode), List.of("dashboard:view"), now, now);
    }
}

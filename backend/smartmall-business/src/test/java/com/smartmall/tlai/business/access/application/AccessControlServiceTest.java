package com.smartmall.tlai.business.access.application;

import com.smartmall.tlai.business.access.domain.AccessRole;
import com.smartmall.tlai.business.access.repository.PermissionAuditRepository;
import com.smartmall.tlai.business.access.repository.RoleRepository;
import com.smartmall.tlai.business.user.domain.BuiltInRoleCodes;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {
    @Mock RoleRepository roleRepository;
    @Mock PermissionAuditRepository auditRepository;
    private AccessControlService service;

    @BeforeEach
    void setUp() {
        service = new AccessControlService(roleRepository, auditRepository);
    }

    @Test
    void shouldProtectAdminPermissions() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole()));

        assertThatThrownBy(() -> service.changePermissions(1L,
                new ChangeRolePermissionsCommand(List.of("dashboard:view"), 2L, "admin2")))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.BUILT_IN_ROLE_PROTECTED));
    }

    @Test
    void shouldProtectAdminEnabledState() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole()));

        assertThatThrownBy(() -> service.changeEnabled(1L,
                new ChangeRoleEnabledCommand(false, 2L, "admin2")))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.BUILT_IN_ROLE_PROTECTED));
    }

    private AccessRole adminRole() {
        OffsetDateTime now = OffsetDateTime.parse("2030-01-01T00:00:00Z");
        return new AccessRole(1L, BuiltInRoleCodes.ADMIN, "系统管理员", "全部权限",
                true, true, List.of("dashboard:view", "system:rbac_manage"), now, now);
    }
}

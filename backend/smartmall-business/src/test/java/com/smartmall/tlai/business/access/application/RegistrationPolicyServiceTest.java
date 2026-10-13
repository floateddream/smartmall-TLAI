package com.smartmall.tlai.business.access.application;

import com.smartmall.tlai.business.access.domain.SystemSetting;
import com.smartmall.tlai.business.access.repository.SystemSettingRepository;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationPolicyServiceTest {
    @Mock SystemSettingRepository settingRepository;

    @Test
    void shouldReadPersistedRegistrationState() {
        var service = new RegistrationPolicyService(settingRepository);
        when(settingRepository.findByKey(RegistrationPolicyService.REGISTRATION_ENABLED_KEY))
                .thenReturn(Optional.of(new SystemSetting(
                        RegistrationPolicyService.REGISTRATION_ENABLED_KEY, "true", 1L,
                        OffsetDateTime.parse("2030-01-01T00:00:00Z"))));

        assertThat(service.isRegistrationEnabled()).isTrue();
    }

    @Test
    void shouldFailClosedWhenSettingIsMissing() {
        var service = new RegistrationPolicyService(settingRepository);
        when(settingRepository.findByKey(RegistrationPolicyService.REGISTRATION_ENABLED_KEY))
                .thenReturn(Optional.empty());

        assertThat(service.isRegistrationEnabled()).isFalse();
        assertThatThrownBy(service::requireRegistrationEnabled)
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.REGISTRATION_CLOSED));
    }

    @Test
    void shouldPersistAdministratorChoice() {
        var service = new RegistrationPolicyService(settingRepository);

        assertThat(service.changeRegistrationEnabled(false, 9L)).isFalse();

        ArgumentCaptor<SystemSetting> captor = ArgumentCaptor.forClass(SystemSetting.class);
        verify(settingRepository).save(captor.capture());
        assertThat(captor.getValue().key()).isEqualTo(RegistrationPolicyService.REGISTRATION_ENABLED_KEY);
        assertThat(captor.getValue().value()).isEqualTo("false");
        assertThat(captor.getValue().updatedBy()).isEqualTo(9L);
    }
}

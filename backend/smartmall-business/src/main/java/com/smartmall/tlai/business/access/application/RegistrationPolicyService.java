package com.smartmall.tlai.business.access.application;

import com.smartmall.tlai.business.access.domain.SystemSetting;
import com.smartmall.tlai.business.access.repository.SystemSettingRepository;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** 统一管理公开注册策略，前端展示和后端注册校验都读取同一个持久化开关。 */
@Service
public class RegistrationPolicyService {
    public static final String REGISTRATION_ENABLED_KEY = "registration.enabled";

    private final SystemSettingRepository settingRepository;

    public RegistrationPolicyService(SystemSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Transactional(readOnly = true)
    public boolean isRegistrationEnabled() {
        // 配置缺失时默认关闭，避免迁移异常导致意外开放注册。
        return settingRepository.findByKey(REGISTRATION_ENABLED_KEY)
                .map(SystemSetting::value)
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public void requireRegistrationEnabled() {
        if (!isRegistrationEnabled()) {
            throw new BusinessException(ErrorCode.REGISTRATION_CLOSED);
        }
    }

    @Transactional
    public boolean changeRegistrationEnabled(boolean enabled, long operatorId) {
        settingRepository.save(new SystemSetting(REGISTRATION_ENABLED_KEY, Boolean.toString(enabled),
                operatorId, OffsetDateTime.now(ZoneOffset.UTC)));
        return enabled;
    }
}

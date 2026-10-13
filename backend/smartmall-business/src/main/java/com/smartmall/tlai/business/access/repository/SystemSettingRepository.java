package com.smartmall.tlai.business.access.repository;

import com.smartmall.tlai.business.access.domain.SystemSetting;

import java.util.Optional;

public interface SystemSettingRepository {
    Optional<SystemSetting> findByKey(String key);
    void save(SystemSetting setting);
}

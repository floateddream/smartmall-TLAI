package com.smartmall.tlai.infrastructure.persistence.access;

import com.smartmall.tlai.business.access.domain.SystemSetting;
import com.smartmall.tlai.business.access.repository.SystemSettingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MybatisSystemSettingRepository implements SystemSettingRepository {
    private final SystemSettingMapper mapper;

    public MybatisSystemSettingRepository(SystemSettingMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<SystemSetting> findByKey(String key) {
        return Optional.ofNullable(mapper.selectByKey(key)).map(this::toDomain);
    }

    @Override
    public void save(SystemSetting setting) {
        mapper.upsert(setting.key(), setting.value(), setting.updatedBy(), setting.updatedAt());
    }

    private SystemSetting toDomain(SystemSettingRow row) {
        return new SystemSetting(row.getSettingKey(), row.getSettingValue(), row.getUpdatedBy(), row.getUpdatedAt());
    }
}

package com.smartmall.tlai.infrastructure.persistence.access;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.OffsetDateTime;

@Mapper
public interface SystemSettingMapper {
    @Select("""
            SELECT setting_key, setting_value, updated_by, updated_at
            FROM sys_system_setting
            WHERE setting_key = #{key}
            """)
    SystemSettingRow selectByKey(@Param("key") String key);

    @Insert("""
            INSERT INTO sys_system_setting (setting_key, setting_value, updated_by, updated_at)
            VALUES (#{key}, #{value}, #{updatedBy}, #{updatedAt})
            ON CONFLICT (setting_key) DO UPDATE
            SET setting_value = EXCLUDED.setting_value,
                updated_by = EXCLUDED.updated_by,
                updated_at = EXCLUDED.updated_at
            """)
    int upsert(@Param("key") String key, @Param("value") String value,
               @Param("updatedBy") Long updatedBy, @Param("updatedAt") OffsetDateTime updatedAt);
}

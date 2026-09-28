package com.smartmall.tlai.infrastructure.persistence.access;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<PermissionPersistenceEntity> {
    @Select("""
            <script>
            SELECT code FROM sys_permission WHERE code IN
            <foreach collection="codes" item="code" open="(" separator="," close=")">#{code}</foreach>
            ORDER BY code
            </script>
            """)
    List<String> selectExistingCodes(@Param("codes") List<String> codes);
}

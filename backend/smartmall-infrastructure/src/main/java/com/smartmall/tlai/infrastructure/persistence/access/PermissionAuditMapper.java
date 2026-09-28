package com.smartmall.tlai.infrastructure.persistence.access;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionAuditMapper extends BaseMapper<PermissionAuditPersistenceEntity> {
    @Select("""
            <script>
            SELECT * FROM sys_permission_audit
            WHERE 1 = 1
            <if test="targetType != null and targetType != ''">AND target_type = #{targetType}</if>
            <if test="targetId != null">AND target_id = #{targetId}</if>
            ORDER BY created_at DESC, id DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<PermissionAuditPersistenceEntity> selectPage(@Param("offset") long offset, @Param("size") int size,
                                                       @Param("targetType") String targetType,
                                                       @Param("targetId") Long targetId);

    @Select("""
            <script>
            SELECT COUNT(*) FROM sys_permission_audit
            WHERE 1 = 1
            <if test="targetType != null and targetType != ''">AND target_type = #{targetType}</if>
            <if test="targetId != null">AND target_id = #{targetId}</if>
            </script>
            """)
    long countAudits(@Param("targetType") String targetType, @Param("targetId") Long targetId);
}

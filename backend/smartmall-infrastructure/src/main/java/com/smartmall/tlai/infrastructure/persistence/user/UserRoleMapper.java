package com.smartmall.tlai.infrastructure.persistence.user;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface UserRoleMapper {
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") long userId);

    @Insert("""
            <script>
            INSERT INTO sys_user_role (user_id, role_id, assigned_by, assigned_at)
            SELECT #{userId}, role.id, #{assignedBy}, #{changedAt}
            FROM sys_role role
            WHERE role.code IN
            <foreach collection="roleCodes" item="code" open="(" separator="," close=")">#{code}</foreach>
            </script>
            """)
    int insertRoles(@Param("userId") long userId, @Param("roleCodes") List<String> roleCodes,
                    @Param("assignedBy") Long assignedBy, @Param("changedAt") OffsetDateTime changedAt);
}

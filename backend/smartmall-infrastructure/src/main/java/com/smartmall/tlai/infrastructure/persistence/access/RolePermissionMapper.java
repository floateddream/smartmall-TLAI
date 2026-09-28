package com.smartmall.tlai.infrastructure.persistence.access;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RolePermissionMapper {
    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") long roleId);

    @Insert("""
            <script>
            INSERT INTO sys_role_permission (role_id, permission_id)
            SELECT #{roleId}, permission.id FROM sys_permission permission
            WHERE permission.code IN
            <foreach collection="permissionCodes" item="code" open="(" separator="," close=")">#{code}</foreach>
            </script>
            """)
    int insertPermissions(@Param("roleId") long roleId,
                          @Param("permissionCodes") List<String> permissionCodes);
}

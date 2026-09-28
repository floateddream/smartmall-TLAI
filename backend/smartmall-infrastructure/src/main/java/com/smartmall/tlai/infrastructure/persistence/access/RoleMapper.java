package com.smartmall.tlai.infrastructure.persistence.access;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<RolePersistenceEntity> {
    String ROLE_ACCESS_SELECT = """
            SELECT r.id, r.code, r.name, r.description, r.built_in, r.enabled, r.created_at, r.updated_at,
                   p.code AS permission_code
            FROM sys_role r
            LEFT JOIN sys_role_permission rp ON rp.role_id = r.id
            LEFT JOIN sys_permission p ON p.id = rp.permission_id
            """;

    @Select(ROLE_ACCESS_SELECT + " ORDER BY r.built_in DESC, r.id")
    List<RoleAccessRow> selectAllWithPermissions();

    @Select(ROLE_ACCESS_SELECT + " WHERE r.id = #{id} ORDER BY p.code")
    List<RoleAccessRow> selectByIdWithPermissions(@Param("id") long id);

    @Select(ROLE_ACCESS_SELECT + " WHERE r.code = #{code} ORDER BY p.code")
    List<RoleAccessRow> selectByCodeWithPermissions(@Param("code") String code);

    @Select("""
            <script>
            SELECT code FROM sys_role WHERE enabled = TRUE AND code IN
            <foreach collection="codes" item="code" open="(" separator="," close=")">#{code}</foreach>
            ORDER BY code
            </script>
            """)
    List<String> selectEnabledCodes(@Param("codes") List<String> codes);

    @Update("UPDATE sys_role SET enabled = #{enabled}, updated_at = #{changedAt} WHERE id = #{id}")
    int updateEnabled(@Param("id") long id, @Param("enabled") boolean enabled,
                      @Param("changedAt") OffsetDateTime changedAt);

    @Update("UPDATE sys_role SET name = #{name}, description = #{description}, updated_at = #{changedAt} WHERE id = #{id}")
    int updateDefinition(@Param("id") long id, @Param("name") String name,
                         @Param("description") String description,
                         @Param("changedAt") OffsetDateTime changedAt);
}

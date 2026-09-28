package com.smartmall.tlai.infrastructure.persistence.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<UserPersistenceEntity> {
    @Select("""
            SELECT u.id, u.username, u.email, u.password_hash, u.enabled, u.created_at, u.updated_at,
                   r.code AS role_code, p.code AS permission_code
            FROM app_user u
            LEFT JOIN sys_user_role ur ON ur.user_id = u.id
            LEFT JOIN sys_role r ON r.id = ur.role_id AND r.enabled = TRUE
            LEFT JOIN sys_role_permission rp ON rp.role_id = r.id
            LEFT JOIN sys_permission p ON p.id = rp.permission_id
            WHERE u.username = #{account} OR u.email = #{normalizedEmail}
            ORDER BY r.code, p.code
            """)
    List<UserAccessRow> selectByLoginWithAccess(@Param("account") String account,
                                                 @Param("normalizedEmail") String normalizedEmail);

    @Select("""
            SELECT u.id, u.username, u.email, u.password_hash, u.enabled, u.created_at, u.updated_at,
                   r.code AS role_code, p.code AS permission_code
            FROM app_user u
            LEFT JOIN sys_user_role ur ON ur.user_id = u.id
            LEFT JOIN sys_role r ON r.id = ur.role_id AND r.enabled = TRUE
            LEFT JOIN sys_role_permission rp ON rp.role_id = r.id
            LEFT JOIN sys_permission p ON p.id = rp.permission_id
            WHERE u.id = #{id}
            ORDER BY r.code, p.code
            """)
    List<UserAccessRow> selectByIdWithAccess(@Param("id") long id);

    @Select("""
            <script>
            SELECT u.id, u.username, u.email, u.password_hash, u.enabled, u.created_at, u.updated_at,
                   r.code AS role_code, p.code AS permission_code
            FROM app_user u
            LEFT JOIN sys_user_role ur ON ur.user_id = u.id
            LEFT JOIN sys_role r ON r.id = ur.role_id AND r.enabled = TRUE
            LEFT JOIN sys_role_permission rp ON rp.role_id = r.id
            LEFT JOIN sys_permission p ON p.id = rp.permission_id
            WHERE u.id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY u.id DESC, r.code, p.code
            </script>
            """)
    List<UserAccessRow> selectByIdsWithAccess(@Param("ids") List<Long> ids);

    @Select("""
            <script>
            SELECT u.id
            FROM app_user u
            WHERE 1 = 1
            <if test="keyword != null and keyword != ''">
              AND (u.username ILIKE CONCAT('%', #{keyword}, '%') OR u.email ILIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="roleCode != null and roleCode != ''">
              AND EXISTS (
                SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.id = ur.role_id
                WHERE ur.user_id = u.id AND r.code = #{roleCode}
              )
            </if>
            <if test="enabled != null">AND u.enabled = #{enabled}</if>
            ORDER BY u.id DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<Long> selectPageIds(@Param("offset") long offset, @Param("size") int size,
                             @Param("keyword") String keyword, @Param("roleCode") String roleCode,
                             @Param("enabled") Boolean enabled);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM app_user u
            WHERE 1 = 1
            <if test="keyword != null and keyword != ''">
              AND (u.username ILIKE CONCAT('%', #{keyword}, '%') OR u.email ILIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="roleCode != null and roleCode != ''">
              AND EXISTS (
                SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.id = ur.role_id
                WHERE ur.user_id = u.id AND r.code = #{roleCode}
              )
            </if>
            <if test="enabled != null">AND u.enabled = #{enabled}</if>
            </script>
            """)
    long countUsers(@Param("keyword") String keyword, @Param("roleCode") String roleCode,
                    @Param("enabled") Boolean enabled);

    @Select("""
            SELECT COUNT(*) FROM app_user u
            JOIN sys_user_role ur ON ur.user_id = u.id
            JOIN sys_role r ON r.id = ur.role_id
            WHERE u.enabled = TRUE AND r.enabled = TRUE AND r.code = #{roleCode}
            """)
    long countEnabledUsersWithRole(@Param("roleCode") String roleCode);

    @Select("""
            SELECT EXISTS (
                SELECT 1 FROM app_user u
                JOIN sys_user_role ur ON ur.user_id = u.id
                JOIN sys_role r ON r.id = ur.role_id
                WHERE u.enabled = TRUE AND r.enabled = TRUE AND r.code = #{roleCode}
            )
            """)
    boolean hasRole(@Param("roleCode") String roleCode);

    @Update("UPDATE app_user SET enabled = #{enabled}, updated_at = #{changedAt} WHERE id = #{id}")
    int updateEnabled(@Param("id") long id, @Param("enabled") boolean enabled,
                      @Param("changedAt") OffsetDateTime changedAt);

    @Select("SELECT pg_advisory_xact_lock(837426501)")
    Object lockAccessControl();
}

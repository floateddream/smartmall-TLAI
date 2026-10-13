package com.smartmall.tlai.infrastructure.persistence.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.tlai.ai.model.AiModelType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;

@Mapper
public interface AiModelConfigMapper extends BaseMapper<AiModelConfigPersistenceEntity> {
    @Select("SELECT pg_advisory_xact_lock(53462401)")
    // PostgreSQL 的 void 函数经 JDBC 读取时会返回字符串值，交给 MyBatis 映射后再忽略即可。
    String lockConfiguration();

    @Update("""
            UPDATE ai_model_config
            SET active = FALSE, updated_by = #{operatorId}, updated_at = #{changedAt}
            WHERE model_type = #{modelType} AND active = TRUE AND owner_user_id IS NULL
            """)
    int clearSystemActive(@Param("modelType") AiModelType modelType,
                          @Param("changedAt") OffsetDateTime changedAt, @Param("operatorId") long operatorId);

    @Update("""
            UPDATE ai_model_config
            SET active = FALSE, updated_by = #{operatorId}, updated_at = #{changedAt}
            WHERE model_type = #{modelType} AND active = TRUE AND owner_user_id = #{ownerUserId}
            """)
    int clearOwnerActive(@Param("modelType") AiModelType modelType, @Param("ownerUserId") long ownerUserId,
                         @Param("changedAt") OffsetDateTime changedAt, @Param("operatorId") long operatorId);

    @Update("""
            UPDATE ai_model_config
            SET active = #{active}, updated_by = #{operatorId}, updated_at = #{changedAt}
            WHERE id = #{id}
            """)
    int updateActive(@Param("id") long id, @Param("active") boolean active,
                     @Param("changedAt") OffsetDateTime changedAt, @Param("operatorId") long operatorId);

    @Update("""
            UPDATE ai_model_config
            SET enabled = #{enabled}, active = CASE WHEN #{enabled} THEN active ELSE FALSE END,
                updated_by = #{operatorId}, updated_at = #{changedAt}
            WHERE id = #{id}
            """)
    int updateEnabled(@Param("id") long id, @Param("enabled") boolean enabled,
                      @Param("changedAt") OffsetDateTime changedAt, @Param("operatorId") long operatorId);
}

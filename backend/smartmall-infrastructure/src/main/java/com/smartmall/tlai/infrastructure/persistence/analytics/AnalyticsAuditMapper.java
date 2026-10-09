package com.smartmall.tlai.infrastructure.persistence.analytics;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AnalyticsAuditMapper extends BaseMapper<AnalyticsAuditPersistenceEntity> {
    @Select("""
            <script>
            SELECT * FROM analytics_query_audit
            WHERE 1 = 1
            <if test="queryMode != null and queryMode != ''">AND query_mode = #{queryMode}</if>
            <if test="status != null and status != ''">AND status = #{status}</if>
            ORDER BY created_at DESC, id DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<AnalyticsAuditPersistenceEntity> selectPage(@Param("offset") long offset, @Param("size") int size,
                                                       @Param("queryMode") String queryMode,
                                                       @Param("status") String status);

    @Select("""
            <script>
            SELECT COUNT(*) FROM analytics_query_audit
            WHERE 1 = 1
            <if test="queryMode != null and queryMode != ''">AND query_mode = #{queryMode}</if>
            <if test="status != null and status != ''">AND status = #{status}</if>
            </script>
            """)
    long countAudits(@Param("queryMode") String queryMode, @Param("status") String status);
}

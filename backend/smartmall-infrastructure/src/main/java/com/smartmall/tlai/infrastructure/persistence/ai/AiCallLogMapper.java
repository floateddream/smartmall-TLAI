package com.smartmall.tlai.infrastructure.persistence.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiCallLogMapper extends BaseMapper<AiCallLogPersistenceEntity> {
    @Select("""
            <script>
            SELECT * FROM ai_call_log
            WHERE 1 = 1
            <if test="modelType != null">AND model_type = #{modelType}</if>
            <if test="provider != null">AND provider = #{provider}</if>
            <if test="scene != null">AND scene = #{scene}</if>
            <if test="status != null">AND status = #{status}</if>
            <if test="traceId != null and traceId != ''">AND trace_id = #{traceId}</if>
            ORDER BY created_at DESC, id DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<AiCallLogPersistenceEntity> selectPage(@Param("offset") long offset,
                                                 @Param("size") int size,
                                                 @Param("modelType") String modelType,
                                                 @Param("provider") String provider,
                                                 @Param("scene") String scene,
                                                 @Param("status") String status,
                                                 @Param("traceId") String traceId);

    @Select("""
            <script>
            SELECT COUNT(*) FROM ai_call_log
            WHERE 1 = 1
            <if test="modelType != null">AND model_type = #{modelType}</if>
            <if test="provider != null">AND provider = #{provider}</if>
            <if test="scene != null">AND scene = #{scene}</if>
            <if test="status != null">AND status = #{status}</if>
            <if test="traceId != null and traceId != ''">AND trace_id = #{traceId}</if>
            </script>
            """)
    long countLogs(@Param("modelType") String modelType,
                   @Param("provider") String provider,
                   @Param("scene") String scene,
                   @Param("status") String status,
                   @Param("traceId") String traceId);
}

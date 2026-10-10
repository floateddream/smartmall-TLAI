package com.smartmall.tlai.infrastructure.persistence.agent;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgentToolAuditMapper extends BaseMapper<AgentToolAuditPersistenceEntity> {
    @Select("""
            <script>
            SELECT * FROM agent_tool_audit
            WHERE 1 = 1
            <if test="toolName != null and toolName != ''">AND tool_name = #{toolName}</if>
            <if test="status != null and status != ''">AND status = #{status}</if>
            <if test="traceId != null and traceId != ''">AND trace_id = #{traceId}</if>
            ORDER BY created_at DESC, id DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<AgentToolAuditPersistenceEntity> selectPage(
            @Param("offset") long offset, @Param("size") int size,
            @Param("toolName") String toolName, @Param("status") String status,
            @Param("traceId") String traceId);

    @Select("""
            <script>
            SELECT COUNT(*) FROM agent_tool_audit
            WHERE 1 = 1
            <if test="toolName != null and toolName != ''">AND tool_name = #{toolName}</if>
            <if test="status != null and status != ''">AND status = #{status}</if>
            <if test="traceId != null and traceId != ''">AND trace_id = #{traceId}</if>
            </script>
            """)
    long countAudits(@Param("toolName") String toolName, @Param("status") String status,
                     @Param("traceId") String traceId);
}

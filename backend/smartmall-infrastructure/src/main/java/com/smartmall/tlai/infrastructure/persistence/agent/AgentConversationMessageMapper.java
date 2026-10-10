package com.smartmall.tlai.infrastructure.persistence.agent;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgentConversationMessageMapper {
    @Insert("""
            INSERT INTO ai_conversation_message
                (conversation_id, user_id, role, content, response_type, model_name, latency_ms,
                 tools_json, suggestions_json, missing_fields_json, created_at)
            VALUES
                (#{conversationId}, #{userId}, #{role}, #{content}, #{responseType}, #{modelName},
                 #{latencyMs}, CAST(#{toolsJson} AS JSONB), CAST(#{suggestionsJson} AS JSONB),
                 CAST(#{missingFieldsJson} AS JSONB), #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentConversationMessagePersistenceEntity entity);

    @Select("""
            SELECT * FROM (
                SELECT message.id, message.conversation_id, message.user_id, message.role,
                       message.content, message.response_type, message.model_name, message.latency_ms,
                       message.tools_json::TEXT AS tools_json,
                       message.suggestions_json::TEXT AS suggestions_json,
                       message.missing_fields_json::TEXT AS missing_fields_json,
                       message.created_at
                FROM ai_conversation_message message
                WHERE message.conversation_id = #{conversationId}
                  AND message.user_id = #{userId}
                  AND EXISTS (
                      SELECT 1 FROM ai_conversation conversation
                      WHERE conversation.id = message.conversation_id
                        AND conversation.user_id = #{userId}
                  )
                ORDER BY message.id DESC
                LIMIT #{limit}
            ) recent
            ORDER BY id ASC
            """)
    List<AgentConversationMessagePersistenceEntity> selectRecent(
            @Param("conversationId") long conversationId,
            @Param("userId") long userId,
            @Param("limit") int limit);
}

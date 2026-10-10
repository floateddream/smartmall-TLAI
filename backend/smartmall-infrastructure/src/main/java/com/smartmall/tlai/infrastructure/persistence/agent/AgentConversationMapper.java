package com.smartmall.tlai.infrastructure.persistence.agent;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface AgentConversationMapper {
    @Insert("""
            INSERT INTO ai_conversation
                (user_id, title, status, state_json, last_message_at, created_at, updated_at)
            VALUES
                (#{userId}, #{title}, #{status}, CAST(#{stateJson} AS JSONB),
                 #{lastMessageAt}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentConversationPersistenceEntity entity);

    @Select("""
            SELECT id, user_id, title, status, state_json::TEXT AS state_json,
                   last_message_at, created_at, updated_at
            FROM ai_conversation
            WHERE id = #{id} AND user_id = #{userId}
            """)
    AgentConversationPersistenceEntity selectOwned(@Param("id") long id, @Param("userId") long userId);

    @Select("""
            SELECT id, user_id, title, status, state_json::TEXT AS state_json,
                   last_message_at, created_at, updated_at
            FROM ai_conversation
            WHERE user_id = #{userId} AND status = 'ACTIVE'
            ORDER BY updated_at DESC, id DESC
            LIMIT #{size} OFFSET #{offset}
            """)
    List<AgentConversationPersistenceEntity> selectOwnedPage(
            @Param("userId") long userId, @Param("offset") long offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM ai_conversation WHERE user_id = #{userId} AND status = 'ACTIVE'")
    long countOwned(@Param("userId") long userId);

    @Update("""
            UPDATE ai_conversation
            SET title = #{title}, state_json = CAST(#{stateJson} AS JSONB),
                last_message_at = #{lastMessageAt}, updated_at = #{lastMessageAt}
            WHERE id = #{id} AND user_id = #{userId} AND status = 'ACTIVE'
            """)
    int updateConversation(@Param("id") long id, @Param("userId") long userId,
                           @Param("title") String title, @Param("stateJson") String stateJson,
                           @Param("lastMessageAt") OffsetDateTime lastMessageAt);

    @Update("""
            UPDATE ai_conversation
            SET status = 'ARCHIVED', state_json = NULL, updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND status = 'ACTIVE'
            """)
    int archive(@Param("id") long id, @Param("userId") long userId);
}

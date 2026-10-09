package com.smartmall.tlai.infrastructure.persistence.knowledge;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocumentPersistenceEntity> {
    @Select("""
            <script>
            SELECT d.*, u.username AS uploader_name
            FROM knowledge_document d
            JOIN app_user u ON u.id = d.uploaded_by
            WHERE 1 = 1
            <if test="keyword != null and keyword != ''">
              AND d.original_name ILIKE CONCAT('%', #{keyword}, '%')
            </if>
            <if test="status != null and status != ''">
              AND d.status = #{status}
            </if>
            ORDER BY d.id DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<KnowledgeDocumentPersistenceEntity> findPage(@Param("offset") long offset,
                                                       @Param("size") int size,
                                                       @Param("keyword") String keyword,
                                                       @Param("status") String status);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM knowledge_document d
            WHERE 1 = 1
            <if test="keyword != null and keyword != ''">
              AND d.original_name ILIKE CONCAT('%', #{keyword}, '%')
            </if>
            <if test="status != null and status != ''">
              AND d.status = #{status}
            </if>
            </script>
            """)
    long countFiltered(@Param("keyword") String keyword, @Param("status") String status);

    @Select("""
            SELECT d.*, u.username AS uploader_name
            FROM knowledge_document d
            JOIN app_user u ON u.id = d.uploaded_by
            WHERE d.id = #{id}
            """)
    KnowledgeDocumentPersistenceEntity findDetailById(@Param("id") long id);

    @Select("""
            SELECT id FROM knowledge_document
            WHERE status = 'UPLOADED'
            ORDER BY id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """)
    Long findNextUploadedIdForUpdate();

    @Update("""
            UPDATE knowledge_document
            SET status = 'PROCESSING', error_message = NULL, updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id} AND status = 'UPLOADED'
            """)
    int markProcessing(@Param("id") long id);

    @Update("""
            UPDATE knowledge_document
            SET status = 'READY', chunk_count = #{chunkCount}, error_message = NULL, updated_at = #{updatedAt}
            WHERE id = #{id} AND status = 'PROCESSING'
            """)
    int markReady(@Param("id") long id, @Param("chunkCount") int chunkCount,
                  @Param("updatedAt") java.time.OffsetDateTime updatedAt);

    @Update("""
            UPDATE knowledge_document
            SET status = 'FAILED', chunk_count = 0, error_message = #{errorMessage}, updated_at = #{updatedAt}
            WHERE id = #{id} AND status = 'PROCESSING'
            """)
    int markFailed(@Param("id") long id, @Param("errorMessage") String errorMessage,
                   @Param("updatedAt") java.time.OffsetDateTime updatedAt);

    @Update("""
            UPDATE knowledge_document
            SET status = 'UPLOADED', chunk_count = 0, error_message = NULL, updated_at = #{updatedAt}
            WHERE id = #{id} AND status = 'FAILED'
            """)
    int retryFailed(@Param("id") long id, @Param("updatedAt") java.time.OffsetDateTime updatedAt);
}

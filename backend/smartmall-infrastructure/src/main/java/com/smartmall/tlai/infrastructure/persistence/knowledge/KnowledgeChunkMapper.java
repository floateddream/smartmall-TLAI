package com.smartmall.tlai.infrastructure.persistence.knowledge;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeChunkMapper {
    @Delete("DELETE FROM knowledge_chunk WHERE document_id = #{documentId}")
    int deleteByDocumentId(@Param("documentId") long documentId);

    @Insert("""
            INSERT INTO knowledge_chunk (document_id, chunk_index, content, token_count, embedding)
            VALUES (#{documentId}, #{chunkIndex}, #{content}, #{tokenCount}, CAST(#{embedding} AS vector))
            """)
    int insert(@Param("documentId") long documentId,
               @Param("chunkIndex") int chunkIndex,
               @Param("content") String content,
               @Param("tokenCount") int tokenCount,
               @Param("embedding") String embedding);

    @Select("""
            SELECT c.id AS chunk_id,
                   c.document_id,
                   d.original_name AS document_name,
                   c.chunk_index,
                   c.content,
                   1 - (c.embedding <=> CAST(#{embedding} AS vector)) AS score
            FROM knowledge_chunk c
            JOIN knowledge_document d ON d.id = c.document_id
            WHERE d.status = 'READY'
              AND c.embedding IS NOT NULL
              AND 1 - (c.embedding <=> CAST(#{embedding} AS vector)) >= #{minScore}
            ORDER BY c.embedding <=> CAST(#{embedding} AS vector), c.id
            LIMIT #{limit}
            """)
    List<KnowledgeSearchPersistenceRow> searchSimilar(@Param("embedding") String embedding,
                                                       @Param("limit") int limit,
                                                       @Param("minScore") double minScore);
}

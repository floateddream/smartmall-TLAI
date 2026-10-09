-- 阶段 5.3：原始文档放在 MinIO，PostgreSQL 保存元数据、文本分段和检索向量。
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE knowledge_document (
    id BIGSERIAL PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    object_key VARCHAR(512) NOT NULL UNIQUE,
    content_type VARCHAR(150) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 20971520),
    file_extension VARCHAR(16) NOT NULL CHECK (file_extension IN ('pdf', 'docx', 'txt', 'md')),
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED'
        CHECK (status IN ('UPLOADED', 'PROCESSING', 'READY', 'FAILED')),
    chunk_count INTEGER NOT NULL DEFAULT 0 CHECK (chunk_count >= 0),
    error_message VARCHAR(1000),
    uploaded_by BIGINT NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_knowledge_document_status_created
    ON knowledge_document (status, created_at DESC);
CREATE INDEX idx_knowledge_document_uploader
    ON knowledge_document (uploaded_by, created_at DESC);

CREATE TABLE knowledge_chunk (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES knowledge_document (id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL CHECK (chunk_index >= 0),
    content TEXT NOT NULL,
    token_count INTEGER NOT NULL DEFAULT 0 CHECK (token_count >= 0),
    embedding vector(1024),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_knowledge_chunk_document ON knowledge_chunk (document_id, chunk_index);
CREATE INDEX idx_knowledge_chunk_embedding_hnsw
    ON knowledge_chunk USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;

INSERT INTO sys_permission (code, name, module, description) VALUES
    ('knowledge:view', '查看知识库', '智能工作台', '查看和下载知识库文档'),
    ('knowledge:manage', '管理知识库', '智能工作台', '上传和删除知识库文档');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role CROSS JOIN sys_permission permission
WHERE role.code IN ('ADMIN', 'OPERATOR')
  AND permission.code IN ('knowledge:view', 'knowledge:manage');

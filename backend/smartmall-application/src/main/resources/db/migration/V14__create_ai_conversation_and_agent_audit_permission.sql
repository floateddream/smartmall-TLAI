CREATE TABLE ai_conversation (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    title VARCHAR(80) NOT NULL DEFAULT '新对话',
    status VARCHAR(12) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    state_json JSONB,
    last_message_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_conversation_user_updated
    ON ai_conversation (user_id, status, updated_at DESC, id DESC);

CREATE TABLE ai_conversation_message (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES ai_conversation (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    role VARCHAR(12) NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content TEXT NOT NULL CHECK (char_length(content) BETWEEN 1 AND 12000),
    response_type VARCHAR(24),
    model_name VARCHAR(120),
    latency_ms BIGINT,
    tools_json JSONB NOT NULL DEFAULT '[]'::JSONB,
    suggestions_json JSONB NOT NULL DEFAULT '[]'::JSONB,
    missing_fields_json JSONB NOT NULL DEFAULT '[]'::JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_conversation_message_order
    ON ai_conversation_message (conversation_id, id);

INSERT INTO sys_permission (code, name, module, description)
VALUES ('agent:audit_view', '查看 Agent 审计', '智能工作台', '查看 Agent 工具调用审计记录')
ON CONFLICT (code) DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
CROSS JOIN sys_permission permission
WHERE role.code = 'ADMIN'
  AND permission.code = 'agent:audit_view'
ON CONFLICT DO NOTHING;

CREATE TABLE ai_call_log (
    id BIGSERIAL PRIMARY KEY,
    model_config_id BIGINT REFERENCES ai_model_config (id) ON DELETE SET NULL,
    model_type VARCHAR(16) NOT NULL CHECK (model_type IN ('CHAT', 'EMBEDDING')),
    provider VARCHAR(32) NOT NULL,
    model_name VARCHAR(120) NOT NULL,
    scene VARCHAR(40) NOT NULL CHECK (scene IN (
        'ASSISTANT_CHAT', 'KNOWLEDGE_RAG', 'INTERNAL_ORCHESTRATION',
        'KNOWLEDGE_EMBEDDING', 'MODEL_CONNECTIVITY_TEST'
    )),
    status VARCHAR(12) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED')),
    latency_ms BIGINT NOT NULL CHECK (latency_ms >= 0),
    input_count INTEGER NOT NULL DEFAULT 0 CHECK (input_count >= 0),
    input_character_count BIGINT NOT NULL DEFAULT 0 CHECK (input_character_count >= 0),
    output_count INTEGER NOT NULL DEFAULT 0 CHECK (output_count >= 0),
    output_unit_count BIGINT CHECK (output_unit_count >= 0),
    error_code VARCHAR(80),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_call_log_created_at ON ai_call_log (created_at DESC, id DESC);
CREATE INDEX idx_ai_call_log_filters ON ai_call_log (model_type, status, scene, created_at DESC);

INSERT INTO sys_permission (code, name, module, description)
VALUES ('ai:call_log_view', '查看 AI 调用日志', '系统设置', '分页筛选查看脱敏后的 AI 模型调用元数据')
ON CONFLICT (code) DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
CROSS JOIN sys_permission permission
WHERE role.code = 'ADMIN'
  AND permission.code = 'ai:call_log_view'
ON CONFLICT DO NOTHING;

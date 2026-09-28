CREATE TABLE ai_model_config (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    provider VARCHAR(32) NOT NULL DEFAULT 'OPENAI_COMPATIBLE'
        CHECK (provider IN ('OPENAI_COMPATIBLE')),
    base_url VARCHAR(500) NOT NULL,
    model_name VARCHAR(120) NOT NULL,
    temperature NUMERIC(3, 2) NOT NULL DEFAULT 0.70
        CHECK (temperature >= 0 AND temperature <= 2),
    max_tokens INTEGER NOT NULL DEFAULT 2048
        CHECK (max_tokens BETWEEN 128 AND 32768),
    active BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT REFERENCES app_user (id) ON DELETE SET NULL,
    updated_by BIGINT REFERENCES app_user (id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_ai_model_config_active
    ON ai_model_config (active) WHERE active = TRUE;

INSERT INTO sys_permission (code, name, module, description)
VALUES ('ai:chat', '使用 AI 助手', '智能工作台', '与当前启用的模型进行对话');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
CROSS JOIN sys_permission permission
WHERE role.code IN ('ADMIN', 'OPERATOR')
  AND permission.code = 'ai:chat';

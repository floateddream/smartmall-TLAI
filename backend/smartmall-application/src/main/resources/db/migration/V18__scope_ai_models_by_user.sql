-- 聊天模型由用户自行维护，系统模型仅用于统一的 Embedding 运行时。
ALTER TABLE ai_model_config
    ADD COLUMN owner_user_id BIGINT REFERENCES app_user (id) ON DELETE CASCADE;

-- 历史聊天模型归属其创建人；极早期缺少创建人的数据归属首个启用管理员。
UPDATE ai_model_config config
SET owner_user_id = COALESCE(
        config.created_by,
        config.updated_by,
        (SELECT user_role.user_id
         FROM sys_user_role user_role
         JOIN sys_role role ON role.id = user_role.role_id
         JOIN app_user app_user ON app_user.id = user_role.user_id
         WHERE role.code = 'ADMIN' AND app_user.enabled = TRUE
         ORDER BY user_role.user_id
         LIMIT 1),
        (SELECT id FROM app_user ORDER BY id LIMIT 1)
    )
WHERE config.model_type = 'CHAT';

ALTER TABLE ai_model_config
    ADD CONSTRAINT ck_ai_model_config_scope CHECK (
        (model_type = 'CHAT' AND owner_user_id IS NOT NULL)
        OR (model_type = 'EMBEDDING' AND owner_user_id IS NULL)
    );

ALTER TABLE ai_model_config DROP CONSTRAINT IF EXISTS ai_model_config_name_key;
DROP INDEX IF EXISTS uk_ai_model_config_active_type;

CREATE UNIQUE INDEX uk_ai_model_config_personal_name
    ON ai_model_config (owner_user_id, name) WHERE owner_user_id IS NOT NULL;
CREATE UNIQUE INDEX uk_ai_model_config_system_name
    ON ai_model_config (name) WHERE owner_user_id IS NULL;
CREATE UNIQUE INDEX uk_ai_model_config_personal_active
    ON ai_model_config (owner_user_id, model_type)
    WHERE active = TRUE AND owner_user_id IS NOT NULL;
CREATE UNIQUE INDEX uk_ai_model_config_system_active
    ON ai_model_config (model_type)
    WHERE active = TRUE AND owner_user_id IS NULL;

CREATE INDEX idx_ai_model_config_owner
    ON ai_model_config (owner_user_id, model_type, updated_at DESC);

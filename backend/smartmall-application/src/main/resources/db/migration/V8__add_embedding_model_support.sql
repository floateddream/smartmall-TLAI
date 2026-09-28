-- 现有模型全部是聊天模型；默认值保证历史数据可以无损迁移。
ALTER TABLE ai_model_config
    ADD COLUMN model_type VARCHAR(16) NOT NULL DEFAULT 'CHAT',
    ADD COLUMN embedding_dimensions INTEGER;

ALTER TABLE ai_model_config
    ADD CONSTRAINT ck_ai_model_config_type
        CHECK (model_type IN ('CHAT', 'EMBEDDING')),
    ADD CONSTRAINT ck_ai_model_config_embedding_dimensions
        CHECK (embedding_dimensions IS NULL OR embedding_dimensions BETWEEN 1 AND 4096);

ALTER TABLE ai_model_config
    DROP CONSTRAINT IF EXISTS ck_ai_model_config_provider;

ALTER TABLE ai_model_config
    ADD CONSTRAINT ck_ai_model_config_provider CHECK (provider IN (
        'OPENAI', 'DEEPSEEK', 'QWEN', 'MOONSHOT', 'ZHIPU', 'OLLAMA', 'OPENAI_COMPATIBLE'
    ));

-- 对话和 Embedding 各自允许有一个活动模型，二者不会互相取消激活状态。
DROP INDEX IF EXISTS uk_ai_model_config_active;

CREATE UNIQUE INDEX uk_ai_model_config_active_type
    ON ai_model_config (model_type) WHERE active = TRUE;

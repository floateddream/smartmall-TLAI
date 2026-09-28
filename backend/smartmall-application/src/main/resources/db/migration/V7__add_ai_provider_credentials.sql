-- V6 已经可能在开发数据库中执行，因此通过新迁移扩展供应商和密钥字段。
ALTER TABLE ai_model_config
    DROP CONSTRAINT IF EXISTS ai_model_config_provider_check;

ALTER TABLE ai_model_config
    ADD CONSTRAINT ck_ai_model_config_provider CHECK (provider IN (
        'OPENAI', 'DEEPSEEK', 'QWEN', 'MOONSHOT', 'ZHIPU', 'OPENAI_COMPATIBLE'
    ));

-- 这里只保存 AES-256-GCM 密文；历史模型需要管理员在页面中补录一次 API Key。
ALTER TABLE ai_model_config
    ADD COLUMN api_key_ciphertext TEXT;

-- 旧记录尚未录入密钥，不能继续作为可调用的活动模型，但记录本身完整保留。
UPDATE ai_model_config
SET active = FALSE
WHERE api_key_ciphertext IS NULL;

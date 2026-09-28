-- 第一阶段用户账户表。业务代码不保存明文密码，只保存 BCrypt 哈希。
CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(32) NOT NULL,
    email VARCHAR(128) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 用户名区分大小写，邮箱由业务层统一转为小写后再保存。
    CONSTRAINT uk_app_user_username UNIQUE (username),
    CONSTRAINT uk_app_user_email UNIQUE (email),
    CONSTRAINT ck_app_user_username CHECK (username ~ '^[A-Za-z0-9_]{4,32}$'),
    CONSTRAINT ck_app_user_email_lowercase CHECK (email = LOWER(email)),
    CONSTRAINT ck_app_user_role CHECK (role IN ('ADMIN', 'OPERATOR'))
);

CREATE INDEX idx_app_user_created_at ON app_user (created_at);

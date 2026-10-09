CREATE TABLE analytics_query_audit (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES app_user (id) ON DELETE SET NULL,
    username VARCHAR(32) NOT NULL,
    query_mode VARCHAR(24) NOT NULL CHECK (query_mode IN ('STRUCTURED', 'NATURAL_LANGUAGE')),
    question VARCHAR(500),
    metric VARCHAR(40),
    dimension VARCHAR(40),
    granularity VARCHAR(20),
    start_date DATE,
    end_date DATE,
    result_limit INTEGER,
    result_count INTEGER,
    cache_hit BOOLEAN NOT NULL DEFAULT FALSE,
    model_name VARCHAR(120),
    latency_ms BIGINT NOT NULL,
    status VARCHAR(12) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED')),
    error_code VARCHAR(80),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_analytics_query_audit_created_at ON analytics_query_audit (created_at DESC);
CREATE INDEX idx_analytics_query_audit_user ON analytics_query_audit (user_id, created_at DESC);

INSERT INTO sys_permission (code, name, module, description)
VALUES ('analytics:audit_view', '查看分析审计', '智能工作台', '查看数据分析查询和模型调用记录')
ON CONFLICT (code) DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_permission permission ON permission.code = 'analytics:audit_view'
WHERE role.code = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

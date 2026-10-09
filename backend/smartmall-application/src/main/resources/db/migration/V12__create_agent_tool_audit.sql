CREATE TABLE agent_tool_audit (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES app_user (id) ON DELETE SET NULL,
    username VARCHAR(32) NOT NULL,
    tool_name VARCHAR(80) NOT NULL,
    arguments_json TEXT NOT NULL,
    status VARCHAR(12) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED')),
    latency_ms BIGINT NOT NULL,
    error_code VARCHAR(80),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_agent_tool_audit_created_at ON agent_tool_audit (created_at DESC);
CREATE INDEX idx_agent_tool_audit_user ON agent_tool_audit (user_id, created_at DESC);
CREATE INDEX idx_agent_tool_audit_tool ON agent_tool_audit (tool_name, created_at DESC);

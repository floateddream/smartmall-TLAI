CREATE TABLE agent_action_confirmation (
    id BIGSERIAL PRIMARY KEY,
    token_hash CHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES app_user (id),
    username VARCHAR(32) NOT NULL,
    action_name VARCHAR(80) NOT NULL,
    arguments_json TEXT NOT NULL,
    status VARCHAR(12) NOT NULL CHECK (status IN ('PENDING', 'EXECUTING', 'SUCCEEDED', 'CANCELLED', 'EXPIRED')),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    result_summary VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_agent_action_user ON agent_action_confirmation (user_id, created_at DESC);
CREATE INDEX idx_agent_action_status_expiry ON agent_action_confirmation (status, expires_at);

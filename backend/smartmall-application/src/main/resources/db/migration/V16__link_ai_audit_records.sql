ALTER TABLE agent_tool_audit
    ADD COLUMN trace_id VARCHAR(36),
    ADD COLUMN conversation_id BIGINT REFERENCES ai_conversation (id) ON DELETE SET NULL;

ALTER TABLE ai_call_log
    ADD COLUMN trace_id VARCHAR(36),
    ADD COLUMN conversation_id BIGINT REFERENCES ai_conversation (id) ON DELETE SET NULL;

CREATE INDEX idx_agent_tool_audit_trace ON agent_tool_audit (trace_id) WHERE trace_id IS NOT NULL;
CREATE INDEX idx_ai_call_log_trace ON ai_call_log (trace_id) WHERE trace_id IS NOT NULL;
CREATE INDEX idx_agent_tool_audit_conversation ON agent_tool_audit (conversation_id) WHERE conversation_id IS NOT NULL;
CREATE INDEX idx_ai_call_log_conversation ON ai_call_log (conversation_id) WHERE conversation_id IS NOT NULL;

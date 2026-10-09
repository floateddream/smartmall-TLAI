package com.smartmall.tlai.agent.model;

import java.time.OffsetDateTime;

/** 工具调用审计只保存受控参数，不保存模型密钥和完整工具结果。 */
public record AgentToolAudit(Long id, long userId, String username, String toolName,
                             String argumentsJson, String status, long latencyMs,
                             String errorCode, OffsetDateTime createdAt) {
}

package com.smartmall.tlai.agent.model;

import java.time.OffsetDateTime;

public record AgentActionConfirmation(Long id, String tokenHash, long userId, String username,
                                      String actionName, String argumentsJson, AgentActionStatus status,
                                      OffsetDateTime expiresAt, String resultSummary,
                                      OffsetDateTime createdAt, OffsetDateTime confirmedAt) {
}

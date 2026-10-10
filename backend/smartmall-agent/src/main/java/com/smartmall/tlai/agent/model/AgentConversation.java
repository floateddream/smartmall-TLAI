package com.smartmall.tlai.agent.model;

import java.time.OffsetDateTime;

public record AgentConversation(Long id, long userId, String title,
                                AgentConversationStatus status, String stateJson,
                                OffsetDateTime lastMessageAt, OffsetDateTime createdAt,
                                OffsetDateTime updatedAt) {
}

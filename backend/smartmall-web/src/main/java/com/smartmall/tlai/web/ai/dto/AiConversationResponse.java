package com.smartmall.tlai.web.ai.dto;

import com.smartmall.tlai.agent.model.AgentConversation;
import com.smartmall.tlai.agent.model.AgentConversationStatus;

import java.time.OffsetDateTime;

public record AiConversationResponse(long id, String title, AgentConversationStatus status,
                                     boolean awaitingParameters, OffsetDateTime lastMessageAt,
                                     OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static AiConversationResponse from(AgentConversation conversation) {
        return new AiConversationResponse(conversation.id(), conversation.title(), conversation.status(),
                conversation.stateJson() != null && !conversation.stateJson().isBlank(),
                conversation.lastMessageAt(), conversation.createdAt(), conversation.updatedAt());
    }
}

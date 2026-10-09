package com.smartmall.tlai.web.ai.dto;

import com.smartmall.tlai.agent.model.AgentPendingAction;

import java.time.OffsetDateTime;

public record AiPendingActionResponse(String token, String actionName, String displayName,
                                      String summary, OffsetDateTime expiresAt) {
    public static AiPendingActionResponse from(AgentPendingAction action) {
        return action == null ? null : new AiPendingActionResponse(action.token(), action.actionName(),
                action.displayName(), action.summary(), action.expiresAt());
    }
}

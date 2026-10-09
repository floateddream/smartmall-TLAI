package com.smartmall.tlai.web.ai.dto;

import com.smartmall.tlai.agent.model.AgentActionExecution;
import com.smartmall.tlai.agent.model.AgentActionStatus;

public record AiAgentActionResponse(String actionName, String displayName, String summary,
                                    AgentActionStatus status, long latencyMs) {
    public static AiAgentActionResponse from(AgentActionExecution execution) {
        return new AiAgentActionResponse(execution.actionName(), execution.displayName(),
                execution.summary(), execution.status(), execution.latencyMs());
    }
}

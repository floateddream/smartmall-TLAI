package com.smartmall.tlai.web.ai.dto;

import com.smartmall.tlai.agent.model.AgentToolExecution;

public record AiAgentToolResponse(String toolName, String displayName, String summary, long latencyMs,
                                  AiAgentNavigationResponse navigation) {
    public static AiAgentToolResponse from(AgentToolExecution execution) {
        return new AiAgentToolResponse(execution.toolName(), execution.displayName(),
                execution.summary(), execution.latencyMs(),
                AiAgentNavigationResponse.from(execution.navigation()));
    }
}

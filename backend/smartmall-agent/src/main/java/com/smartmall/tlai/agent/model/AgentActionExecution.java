package com.smartmall.tlai.agent.model;

public record AgentActionExecution(String actionName, String displayName, String summary,
                                   AgentActionStatus status, long latencyMs) {
}

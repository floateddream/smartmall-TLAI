package com.smartmall.tlai.agent.model;

public record AgentToolExecution(String toolName, String displayName, String summary, long latencyMs) {
}

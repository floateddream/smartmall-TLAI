package com.smartmall.tlai.agent.model;

public record AgentToolExecution(String toolName, String displayName, String summary, long latencyMs,
                                 AgentPageNavigation navigation) {
    /** 兼容既有调用点以及数据库中不包含 navigation 字段的历史 JSON。 */
    public AgentToolExecution(String toolName, String displayName, String summary, long latencyMs) {
        this(toolName, displayName, summary, latencyMs, null);
    }
}

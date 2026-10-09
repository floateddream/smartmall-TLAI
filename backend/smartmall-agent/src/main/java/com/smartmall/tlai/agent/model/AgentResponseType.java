package com.smartmall.tlai.agent.model;

/**
 * Agent 响应类型用于指导前端采用合适的展示方式，不参与任何权限或执行判断。
 */
public enum AgentResponseType {
    ANSWER,
    TOOL_RESULT,
    ACTION_CONFIRMATION,
    CLARIFICATION,
    UNSUPPORTED,
    SYSTEM_ERROR
}

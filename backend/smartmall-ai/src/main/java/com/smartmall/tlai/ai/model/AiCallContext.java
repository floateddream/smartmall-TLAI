package com.smartmall.tlai.ai.model;

/** 一次业务请求的安全关联标识，不包含提示词、密钥或业务数据。 */
public record AiCallContext(long userId, String traceId, Long conversationId) {
    public AiCallContext {
        if (userId <= 0) {
            throw new IllegalArgumentException("AI 调用用户标识不合法");
        }
        if (traceId != null && !traceId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")) {
            throw new IllegalArgumentException("AI 调用追踪标识不合法");
        }
        if (conversationId != null && conversationId <= 0) {
            throw new IllegalArgumentException("AI 会话标识不合法");
        }
    }
}

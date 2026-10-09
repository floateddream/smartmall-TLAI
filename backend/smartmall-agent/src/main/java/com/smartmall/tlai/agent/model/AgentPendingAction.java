package com.smartmall.tlai.agent.model;

import java.time.OffsetDateTime;

/** 返回给前端的待确认操作；原始令牌只在创建时出现一次。 */
public record AgentPendingAction(String token, String actionName, String displayName,
                                 String summary, OffsetDateTime expiresAt) {
}

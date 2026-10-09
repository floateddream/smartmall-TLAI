package com.smartmall.tlai.agent.model;

import java.util.Set;

/** 当前登录用户的可信快照，工具参数不能覆盖这里的身份与权限。 */
public record AgentUserContext(long userId, String username, Set<String> permissions) {
    public AgentUserContext {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public boolean can(String permission) {
        return permissions.contains(permission);
    }
}

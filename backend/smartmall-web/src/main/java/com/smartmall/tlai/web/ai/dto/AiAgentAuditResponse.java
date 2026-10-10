package com.smartmall.tlai.web.ai.dto;

import com.smartmall.tlai.agent.model.AgentToolAudit;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;

public record AiAgentAuditResponse(long id, Long userId, String username, String toolName,
                                   List<String> argumentKeys, String status, long latencyMs,
                                   String errorCode, String traceId, Long conversationId,
                                   OffsetDateTime createdAt) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static AiAgentAuditResponse from(AgentToolAudit audit) {
        return new AiAgentAuditResponse(audit.id(), audit.userId(), audit.username(), audit.toolName(),
                argumentKeys(audit.argumentsJson()), audit.status(), audit.latencyMs(), audit.errorCode(),
                audit.traceId(), audit.conversationId(), audit.createdAt());
    }

    private static List<String> argumentKeys(String argumentsJson) {
        try {
            var node = OBJECT_MAPPER.readTree(argumentsJson);
            if (node == null || !node.isObject()) return List.of();
            return java.util.stream.StreamSupport.stream(
                            java.util.Spliterators.spliteratorUnknownSize(node.fieldNames(), 0), false)
                    .filter(key -> key.matches("[A-Za-z][A-Za-z0-9]{0,39}"))
                    .sorted()
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }
}

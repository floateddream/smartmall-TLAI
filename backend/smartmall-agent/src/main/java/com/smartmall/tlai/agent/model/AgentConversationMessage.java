package com.smartmall.tlai.agent.model;

import com.smartmall.tlai.ai.model.AiMessageRole;

import java.time.OffsetDateTime;
import java.util.List;

public record AgentConversationMessage(Long id, long conversationId, long userId,
                                       AiMessageRole role, String content,
                                       AgentResponseType responseType, String modelName,
                                       Long latencyMs, List<AgentToolExecution> tools,
                                       List<AgentSuggestion> suggestions,
                                       List<String> missingFields,
                                       OffsetDateTime createdAt) {
    public AgentConversationMessage {
        tools = tools == null ? List.of() : List.copyOf(tools);
        suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
        missingFields = missingFields == null ? List.of() : List.copyOf(missingFields);
    }
}

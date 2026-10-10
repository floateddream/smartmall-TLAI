package com.smartmall.tlai.agent.model;

import java.util.List;

public record AgentChatResult(String content, String modelName, long latencyMs,
                              List<AgentToolExecution> tools,
                              AgentPendingAction pendingAction,
                              List<AgentSuggestion> suggestions,
                              AgentResponseType responseType,
                              List<String> missingFields,
                              AgentConversationState conversationState) {
    public AgentChatResult {
        tools = tools == null ? List.of() : List.copyOf(tools);
        suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
        responseType = responseType == null ? AgentResponseType.ANSWER : responseType;
        missingFields = missingFields == null ? List.of() : List.copyOf(missingFields);
    }

    public AgentChatResult(String content, String modelName, long latencyMs,
                           List<AgentToolExecution> tools, AgentPendingAction pendingAction,
                           List<AgentSuggestion> suggestions, AgentResponseType responseType,
                           List<String> missingFields) {
        this(content, modelName, latencyMs, tools, pendingAction, suggestions,
                responseType, missingFields, null);
    }

    public AgentChatResult(String content, String modelName, long latencyMs,
                           List<AgentToolExecution> tools) {
        this(content, modelName, latencyMs, tools, null, List.of(),
                AgentResponseType.TOOL_RESULT, List.of(), null);
    }
}

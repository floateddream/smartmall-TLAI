package com.smartmall.tlai.web.ai.dto;

import com.smartmall.tlai.ai.model.AiChatResult;
import com.smartmall.tlai.agent.model.AgentChatResult;
import com.smartmall.tlai.agent.model.AgentResponseType;
import com.smartmall.tlai.rag.model.RagChatResult;

import java.util.List;

public record AiChatResponse(String content, String modelName, long latencyMs,
                             List<AiChatSourceResponse> sources,
                             List<AiAgentToolResponse> tools,
                             AiPendingActionResponse pendingAction,
                             List<AiSuggestionResponse> suggestions,
                             AgentResponseType responseType,
                             List<String> missingFields) {
    public static AiChatResponse from(AiChatResult result) {
        return new AiChatResponse(result.content(), result.modelName(), result.latencyMs(),
                List.of(), List.of(), null, List.of(), AgentResponseType.ANSWER, List.of());
    }

    public static AiChatResponse from(RagChatResult result) {
        return new AiChatResponse(result.content(), result.modelName(), result.latencyMs(),
                result.sources().stream().map(AiChatSourceResponse::from).toList(), List.of(), null, List.of(),
                AgentResponseType.TOOL_RESULT, List.of());
    }

    public static AiChatResponse from(AgentChatResult result) {
        return new AiChatResponse(result.content(), result.modelName(), result.latencyMs(), List.of(),
                result.tools().stream().map(AiAgentToolResponse::from).toList(),
                AiPendingActionResponse.from(result.pendingAction()),
                result.suggestions().stream().map(AiSuggestionResponse::from).toList(),
                result.responseType(), result.missingFields());
    }
}

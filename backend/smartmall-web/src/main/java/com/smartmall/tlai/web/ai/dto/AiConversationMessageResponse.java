package com.smartmall.tlai.web.ai.dto;

import com.smartmall.tlai.agent.model.AgentConversationMessage;
import com.smartmall.tlai.agent.model.AgentResponseType;
import com.smartmall.tlai.ai.model.AiMessageRole;

import java.time.OffsetDateTime;
import java.util.List;

public record AiConversationMessageResponse(long id, AiMessageRole role, String content,
                                            AgentResponseType responseType, String modelName,
                                            Long latencyMs, List<AiAgentToolResponse> tools,
                                            List<AiSuggestionResponse> suggestions,
                                            List<String> missingFields, OffsetDateTime createdAt) {
    public static AiConversationMessageResponse from(AgentConversationMessage message) {
        return new AiConversationMessageResponse(message.id(), message.role(), message.content(),
                message.responseType(), message.modelName(), message.latencyMs(),
                message.tools().stream().map(AiAgentToolResponse::from).toList(),
                message.suggestions().stream().map(AiSuggestionResponse::from).toList(),
                message.missingFields(), message.createdAt());
    }
}

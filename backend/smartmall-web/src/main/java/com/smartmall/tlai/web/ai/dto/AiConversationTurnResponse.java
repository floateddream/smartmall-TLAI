package com.smartmall.tlai.web.ai.dto;

import com.smartmall.tlai.agent.model.AgentConversationTurn;

public record AiConversationTurnResponse(AiConversationResponse conversation,
                                         long userMessageId,
                                         long assistantMessageId,
                                         AiChatResponse reply) {
    public static AiConversationTurnResponse from(AgentConversationTurn turn) {
        return new AiConversationTurnResponse(AiConversationResponse.from(turn.conversation()),
                turn.userMessage().id(), turn.assistantMessage().id(), AiChatResponse.from(turn.reply()));
    }
}

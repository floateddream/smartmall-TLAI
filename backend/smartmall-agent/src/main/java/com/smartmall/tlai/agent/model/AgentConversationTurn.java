package com.smartmall.tlai.agent.model;

public record AgentConversationTurn(AgentConversation conversation,
                                    AgentConversationMessage userMessage,
                                    AgentConversationMessage assistantMessage,
                                    AgentChatResult reply) {
}

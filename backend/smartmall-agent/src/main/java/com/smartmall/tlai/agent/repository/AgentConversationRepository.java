package com.smartmall.tlai.agent.repository;

import com.smartmall.tlai.agent.model.AgentConversation;
import com.smartmall.tlai.agent.model.AgentConversationMessage;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface AgentConversationRepository {
    AgentConversation insertConversation(AgentConversation conversation);

    Optional<AgentConversation> findOwnedConversation(long id, long userId);

    List<AgentConversation> findOwnedConversations(long userId, int page, int size);

    long countOwnedConversations(long userId);

    void updateConversation(long id, long userId, String title, String stateJson,
                            OffsetDateTime lastMessageAt);

    boolean archiveConversation(long id, long userId);

    AgentConversationMessage insertMessage(AgentConversationMessage message);

    List<AgentConversationMessage> findRecentMessages(long conversationId, long userId, int limit);

    List<AgentConversationMessage> findMessages(long conversationId, long userId, int limit);
}

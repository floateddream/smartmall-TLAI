package com.smartmall.tlai.agent.repository;

import com.smartmall.tlai.agent.model.AgentActionConfirmation;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface AgentActionConfirmationRepository {
    AgentActionConfirmation insert(AgentActionConfirmation confirmation);

    Optional<AgentActionConfirmation> findByTokenHash(String tokenHash);

    boolean markExecuting(long id, long userId, OffsetDateTime confirmedAt);

    void markSucceeded(long id, String resultSummary);

    boolean cancel(long id, long userId);

    boolean markExpired(long id);
}

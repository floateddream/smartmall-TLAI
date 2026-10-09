package com.smartmall.tlai.agent.application;

import com.smartmall.tlai.agent.model.AgentToolAudit;
import com.smartmall.tlai.agent.model.AgentUserContext;
import com.smartmall.tlai.agent.repository.AgentToolAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class AgentToolAuditService {
    private static final int MAX_ARGUMENTS_LENGTH = 4000;
    private final AgentToolAuditRepository repository;

    public AgentToolAuditService(AgentToolAuditRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AgentUserContext user, String toolName, String argumentsJson,
                       String status, long latencyMs, String errorCode) {
        String arguments = argumentsJson == null ? "{}" : argumentsJson;
        if (arguments.length() > MAX_ARGUMENTS_LENGTH) {
            arguments = arguments.substring(0, MAX_ARGUMENTS_LENGTH);
        }
        repository.insert(new AgentToolAudit(null, user.userId(), user.username(), toolName,
                arguments, status, latencyMs, errorCode, OffsetDateTime.now(ZoneOffset.UTC)));
    }
}

package com.smartmall.tlai.infrastructure.persistence.agent;

import com.smartmall.tlai.agent.model.AgentToolAudit;
import com.smartmall.tlai.agent.repository.AgentToolAuditRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisAgentToolAuditRepository implements AgentToolAuditRepository {
    private final AgentToolAuditMapper mapper;

    public MybatisAgentToolAuditRepository(AgentToolAuditMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void insert(AgentToolAudit audit) {
        AgentToolAuditPersistenceEntity entity = new AgentToolAuditPersistenceEntity();
        entity.setUserId(audit.userId());
        entity.setUsername(audit.username());
        entity.setToolName(audit.toolName());
        entity.setArgumentsJson(audit.argumentsJson());
        entity.setStatus(audit.status());
        entity.setLatencyMs(audit.latencyMs());
        entity.setErrorCode(audit.errorCode());
        entity.setCreatedAt(audit.createdAt());
        mapper.insert(entity);
    }
}

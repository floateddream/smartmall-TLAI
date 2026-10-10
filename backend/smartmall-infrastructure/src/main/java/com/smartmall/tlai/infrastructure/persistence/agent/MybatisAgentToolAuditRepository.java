package com.smartmall.tlai.infrastructure.persistence.agent;

import com.smartmall.tlai.agent.model.AgentToolAudit;
import com.smartmall.tlai.agent.repository.AgentToolAuditRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
        entity.setTraceId(audit.traceId());
        entity.setConversationId(audit.conversationId());
        entity.setCreatedAt(audit.createdAt());
        mapper.insert(entity);
    }

    @Override
    public List<AgentToolAudit> findPage(int page, int size, String toolName, String status, String traceId) {
        long offset = ((long) page - 1) * size;
        return mapper.selectPage(offset, size, toolName, status, traceId).stream().map(this::toDomain).toList();
    }

    @Override
    public long count(String toolName, String status, String traceId) {
        return mapper.countAudits(toolName, status, traceId);
    }

    @Override
    public Optional<AgentToolAudit> findById(long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    private AgentToolAudit toDomain(AgentToolAuditPersistenceEntity entity) {
        return new AgentToolAudit(entity.getId(), entity.getUserId(), entity.getUsername(),
                entity.getToolName(), entity.getArgumentsJson(), entity.getStatus(),
                entity.getLatencyMs(), entity.getErrorCode(), entity.getTraceId(),
                entity.getConversationId(), entity.getCreatedAt());
    }
}

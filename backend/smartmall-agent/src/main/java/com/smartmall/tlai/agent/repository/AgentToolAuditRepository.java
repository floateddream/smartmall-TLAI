package com.smartmall.tlai.agent.repository;

import com.smartmall.tlai.agent.model.AgentToolAudit;

import java.util.List;
import java.util.Optional;

public interface AgentToolAuditRepository {
    void insert(AgentToolAudit audit);

    List<AgentToolAudit> findPage(int page, int size, String toolName, String status, String traceId);

    long count(String toolName, String status, String traceId);

    Optional<AgentToolAudit> findById(long id);
}

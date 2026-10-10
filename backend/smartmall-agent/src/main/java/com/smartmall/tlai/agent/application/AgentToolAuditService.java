package com.smartmall.tlai.agent.application;

import com.smartmall.tlai.agent.model.AgentToolAudit;
import com.smartmall.tlai.agent.model.AgentUserContext;
import com.smartmall.tlai.agent.repository.AgentToolAuditRepository;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import com.smartmall.tlai.ai.model.AiCallContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;

@Service
public class AgentToolAuditService {
    private static final int MAX_ARGUMENTS_LENGTH = 4000;
    private static final Set<String> STATUSES = Set.of("SUCCESS", "FAILED");
    private final AgentToolAuditRepository repository;

    public AgentToolAuditService(AgentToolAuditRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AgentUserContext user, String toolName, String argumentsJson,
                       String status, long latencyMs, String errorCode) {
        record(user, toolName, argumentsJson, status, latencyMs, errorCode, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AgentUserContext user, String toolName, String argumentsJson,
                       String status, long latencyMs, String errorCode, AiCallContext context) {
        String arguments = argumentsJson == null ? "{}" : argumentsJson;
        if (arguments.length() > MAX_ARGUMENTS_LENGTH) {
            arguments = arguments.substring(0, MAX_ARGUMENTS_LENGTH);
        }
        repository.insert(new AgentToolAudit(null, user.userId(), user.username(), toolName,
                arguments, status, latencyMs, errorCode,
                context == null ? null : context.traceId(),
                context == null ? null : context.conversationId(),
                OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Transactional(readOnly = true)
    public PageResponse<AgentToolAudit> list(int page, int size, String toolName, String status) {
        return list(page, size, toolName, status, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<AgentToolAudit> list(int page, int size, String toolName, String status,
                                             String traceId) {
        String normalizedTool = toolName == null || toolName.isBlank() ? null : toolName.trim();
        if (normalizedTool != null && normalizedTool.length() > 80) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工具名称筛选条件过长");
        }
        String normalizedStatus = null;
        if (status != null && !status.isBlank()) {
            normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
            if (!STATUSES.contains(normalizedStatus)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "审计状态不合法");
            }
        }
        String normalizedTraceId = normalizeTraceId(traceId);
        return new PageResponse<>(repository.findPage(page, size, normalizedTool, normalizedStatus,
                normalizedTraceId), repository.count(normalizedTool, normalizedStatus, normalizedTraceId),
                page, size);
    }

    @Transactional(readOnly = true)
    public AgentToolAudit get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Agent 审计记录不存在"));
    }

    private static String normalizeTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) return null;
        String normalized = traceId.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "追踪标识不合法");
        }
        return normalized;
    }
}

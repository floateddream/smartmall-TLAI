package com.smartmall.tlai.web.access.dto;

import com.smartmall.tlai.business.access.domain.PermissionAudit;

import java.time.OffsetDateTime;

public record PermissionAuditResponse(Long id, Long operatorId, String operatorName,
                                      String targetType, Long targetId, String targetName,
                                      String action, String beforeValue, String afterValue,
                                      OffsetDateTime createdAt) {
    public static PermissionAuditResponse from(PermissionAudit audit) {
        return new PermissionAuditResponse(audit.id(), audit.operatorId(), audit.operatorName(),
                audit.targetType(), audit.targetId(), audit.targetName(), audit.action(),
                audit.beforeValue(), audit.afterValue(), audit.createdAt());
    }
}

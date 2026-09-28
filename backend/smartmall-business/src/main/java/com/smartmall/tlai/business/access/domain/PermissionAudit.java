package com.smartmall.tlai.business.access.domain;

import java.time.OffsetDateTime;

public record PermissionAudit(Long id, Long operatorId, String operatorName,
                              String targetType, Long targetId, String targetName,
                              String action, String beforeValue, String afterValue,
                              OffsetDateTime createdAt) {
}

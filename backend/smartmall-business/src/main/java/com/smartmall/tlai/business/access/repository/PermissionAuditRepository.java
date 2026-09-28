package com.smartmall.tlai.business.access.repository;

import com.smartmall.tlai.business.access.domain.PermissionAudit;

import java.util.List;

public interface PermissionAuditRepository {
    void insert(PermissionAudit audit);
    long count(String targetType, Long targetId);
    List<PermissionAudit> findPage(int page, int size, String targetType, Long targetId);
}

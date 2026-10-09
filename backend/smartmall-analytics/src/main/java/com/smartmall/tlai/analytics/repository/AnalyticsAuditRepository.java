package com.smartmall.tlai.analytics.repository;

import com.smartmall.tlai.analytics.model.AnalyticsAudit;

import java.util.List;

public interface AnalyticsAuditRepository {
    void insert(AnalyticsAudit audit);
    long count(String queryMode, String status);
    List<AnalyticsAudit> findPage(int page, int size, String queryMode, String status);
}

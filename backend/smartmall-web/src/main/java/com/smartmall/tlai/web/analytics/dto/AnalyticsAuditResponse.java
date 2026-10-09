package com.smartmall.tlai.web.analytics.dto;

import com.smartmall.tlai.analytics.model.AnalyticsAudit;
import com.smartmall.tlai.analytics.model.AnalyticsDimension;
import com.smartmall.tlai.analytics.model.AnalyticsGranularity;
import com.smartmall.tlai.analytics.model.AnalyticsMetric;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AnalyticsAuditResponse(
        Long id,
        Long userId,
        String username,
        String queryMode,
        String question,
        AnalyticsMetric metric,
        AnalyticsDimension dimension,
        AnalyticsGranularity granularity,
        LocalDate startDate,
        LocalDate endDate,
        Integer resultLimit,
        Integer resultCount,
        boolean cacheHit,
        String modelName,
        long latencyMs,
        String status,
        String errorCode,
        OffsetDateTime createdAt
) {
    public static AnalyticsAuditResponse from(AnalyticsAudit audit) {
        return new AnalyticsAuditResponse(audit.id(), audit.userId(), audit.username(), audit.queryMode(),
                audit.question(), audit.metric(), audit.dimension(), audit.granularity(), audit.startDate(),
                audit.endDate(), audit.resultLimit(), audit.resultCount(), audit.cacheHit(), audit.modelName(),
                audit.latencyMs(), audit.status(), audit.errorCode(), audit.createdAt());
    }
}

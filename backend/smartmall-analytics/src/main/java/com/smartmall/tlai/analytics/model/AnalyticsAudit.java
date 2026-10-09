package com.smartmall.tlai.analytics.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 一次 BI 请求的安全审计摘要，不保存模型密钥或完整提示词。 */
public record AnalyticsAudit(
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
}

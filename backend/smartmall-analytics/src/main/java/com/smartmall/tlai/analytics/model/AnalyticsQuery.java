package com.smartmall.tlai.analytics.model;

import java.time.LocalDate;

/** 受控分析 DSL：只描述白名单指标和维度，不携带 SQL。 */
public record AnalyticsQuery(
        AnalyticsMetric metric,
        AnalyticsDimension dimension,
        AnalyticsGranularity granularity,
        LocalDate startDate,
        LocalDate endDate,
        int limit
) {
}

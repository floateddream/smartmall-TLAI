package com.smartmall.tlai.web.analytics.dto;

import com.smartmall.tlai.analytics.model.AnalyticsDimension;
import com.smartmall.tlai.analytics.model.AnalyticsGranularity;
import com.smartmall.tlai.analytics.model.AnalyticsMetric;
import com.smartmall.tlai.analytics.model.AnalyticsQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AnalyticsQueryRequest(
        @NotNull AnalyticsMetric metric,
        @NotNull AnalyticsDimension dimension,
        AnalyticsGranularity granularity,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @Min(1) @Max(50) Integer limit
) {
    public AnalyticsQuery toDomain() {
        return new AnalyticsQuery(metric, dimension,
                granularity == null ? AnalyticsGranularity.DAY : granularity,
                startDate, endDate, limit == null ? 20 : limit);
    }
}

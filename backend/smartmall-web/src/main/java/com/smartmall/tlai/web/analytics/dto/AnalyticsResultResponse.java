package com.smartmall.tlai.web.analytics.dto;

import com.smartmall.tlai.analytics.model.AnalyticsDimension;
import com.smartmall.tlai.analytics.model.AnalyticsGranularity;
import com.smartmall.tlai.analytics.model.AnalyticsMetric;
import com.smartmall.tlai.analytics.model.AnalyticsResult;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record AnalyticsResultResponse(
        AnalyticsMetric metric,
        AnalyticsDimension dimension,
        AnalyticsGranularity granularity,
        LocalDate startDate,
        LocalDate endDate,
        int limit,
        List<AnalyticsPointResponse> points,
        OffsetDateTime generatedAt,
        boolean cached
) {
    public static AnalyticsResultResponse from(AnalyticsResult result) {
        var query = result.query();
        return new AnalyticsResultResponse(query.metric(), query.dimension(), query.granularity(),
                query.startDate(), query.endDate(), query.limit(),
                result.points().stream().map(AnalyticsPointResponse::from).toList(), result.generatedAt(),
                result.cached());
    }
}

package com.smartmall.tlai.web.analytics.dto;

import com.smartmall.tlai.analytics.model.AnalyticsDimension;
import com.smartmall.tlai.analytics.model.AnalyticsGranularity;
import com.smartmall.tlai.analytics.model.AnalyticsMetric;
import com.smartmall.tlai.analytics.model.NaturalLanguageAnalyticsResult;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record NaturalLanguageAnalyticsResponse(
        String question,
        AnalyticsMetric metric,
        AnalyticsDimension dimension,
        AnalyticsGranularity granularity,
        LocalDate startDate,
        LocalDate endDate,
        int limit,
        List<AnalyticsPointResponse> points,
        OffsetDateTime generatedAt,
        boolean cached,
        String explanation,
        String modelName,
        long latencyMs
) {
    public static NaturalLanguageAnalyticsResponse from(NaturalLanguageAnalyticsResult result) {
        var analytics = result.analytics();
        var query = analytics.query();
        return new NaturalLanguageAnalyticsResponse(result.question(), query.metric(), query.dimension(),
                query.granularity(), query.startDate(), query.endDate(), query.limit(),
                analytics.points().stream().map(AnalyticsPointResponse::from).toList(), analytics.generatedAt(),
                analytics.cached(), result.explanation(), result.modelName(), result.latencyMs());
    }
}

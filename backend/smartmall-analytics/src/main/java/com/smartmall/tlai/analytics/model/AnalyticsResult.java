package com.smartmall.tlai.analytics.model;

import java.time.OffsetDateTime;
import java.util.List;

public record AnalyticsResult(AnalyticsQuery query, List<AnalyticsDataPoint> points,
                              OffsetDateTime generatedAt, boolean cached) {
    public AnalyticsResult {
        points = List.copyOf(points);
    }

    public AnalyticsResult(AnalyticsQuery query, List<AnalyticsDataPoint> points, OffsetDateTime generatedAt) {
        this(query, points, generatedAt, false);
    }
}

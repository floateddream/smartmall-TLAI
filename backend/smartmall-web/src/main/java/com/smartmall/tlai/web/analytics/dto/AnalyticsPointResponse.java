package com.smartmall.tlai.web.analytics.dto;

import com.smartmall.tlai.analytics.model.AnalyticsDataPoint;

import java.math.BigDecimal;

public record AnalyticsPointResponse(String label, BigDecimal value) {
    public static AnalyticsPointResponse from(AnalyticsDataPoint point) {
        return new AnalyticsPointResponse(point.label(), point.value());
    }
}

package com.smartmall.tlai.analytics.model;

import java.util.Locale;

public enum AnalyticsGranularity {
    DAY,
    WEEK,
    MONTH;

    public String sqlValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}

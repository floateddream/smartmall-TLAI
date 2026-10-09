package com.smartmall.tlai.infrastructure.persistence.analytics;

import java.math.BigDecimal;

public class AnalyticsPointRow {
    private String label;
    private BigDecimal metricValue;

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public BigDecimal getMetricValue() { return metricValue; }
    public void setMetricValue(BigDecimal metricValue) { this.metricValue = metricValue; }
}

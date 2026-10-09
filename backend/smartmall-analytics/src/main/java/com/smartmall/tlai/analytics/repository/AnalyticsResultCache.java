package com.smartmall.tlai.analytics.repository;

import com.smartmall.tlai.analytics.model.AnalyticsQuery;
import com.smartmall.tlai.analytics.model.AnalyticsResult;

import java.util.Optional;

public interface AnalyticsResultCache {
    Optional<AnalyticsResult> find(AnalyticsQuery query);
    void put(AnalyticsResult result);
}

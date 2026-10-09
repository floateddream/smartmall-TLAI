package com.smartmall.tlai.analytics.repository;

import com.smartmall.tlai.analytics.model.AnalyticsDataPoint;
import com.smartmall.tlai.analytics.model.AnalyticsGranularity;

import java.time.OffsetDateTime;
import java.util.List;

public interface AnalyticsRepository {
    List<AnalyticsDataPoint> findOrderCountTrend(OffsetDateTime start, OffsetDateTime end,
                                                 AnalyticsGranularity granularity, int limit);
    List<AnalyticsDataPoint> findRefundAmountTrend(OffsetDateTime start, OffsetDateTime end,
                                                   AnalyticsGranularity granularity, int limit);
    List<AnalyticsDataPoint> findRefundAmountByProduct(OffsetDateTime start, OffsetDateTime end, int limit);
    List<AnalyticsDataPoint> findRefundCountByReason(OffsetDateTime start, OffsetDateTime end, int limit);
    List<AnalyticsDataPoint> findTicketCountTrend(OffsetDateTime start, OffsetDateTime end,
                                                  AnalyticsGranularity granularity, int limit);
    List<AnalyticsDataPoint> findTicketCountByStatus(OffsetDateTime start, OffsetDateTime end, int limit);
    List<AnalyticsDataPoint> findTicketCountByCategory(OffsetDateTime start, OffsetDateTime end, int limit);
}

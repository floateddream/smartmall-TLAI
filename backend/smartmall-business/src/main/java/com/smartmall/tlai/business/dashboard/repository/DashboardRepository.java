package com.smartmall.tlai.business.dashboard.repository;

import com.smartmall.tlai.business.dashboard.model.DashboardMetrics;
import com.smartmall.tlai.business.dashboard.model.DashboardRecentOrder;
import com.smartmall.tlai.business.dashboard.model.DashboardTrendPoint;

import java.time.OffsetDateTime;
import java.util.List;

public interface DashboardRepository {
    DashboardMetrics findMetrics(OffsetDateTime currentStart, OffsetDateTime currentEnd,
                                 OffsetDateTime previousStart, OffsetDateTime previousEnd);

    List<DashboardTrendPoint> findTrend(OffsetDateTime start, OffsetDateTime end);

    List<DashboardRecentOrder> findRecentOrders(int limit);
}

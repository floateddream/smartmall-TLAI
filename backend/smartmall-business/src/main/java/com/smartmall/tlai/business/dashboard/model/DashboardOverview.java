package com.smartmall.tlai.business.dashboard.model;

import java.util.List;

public record DashboardOverview(
        long weekOrders,
        double weekOrderChangePercent,
        long weekRefunds,
        double weekRefundChangePercent,
        long pendingTickets,
        double weekPaymentRate,
        double weekPaymentRateChangePoints,
        List<DashboardTrendPoint> trend,
        List<DashboardRecentOrder> recentOrders,
        java.time.OffsetDateTime generatedAt
) {
    public DashboardOverview {
        trend = List.copyOf(trend);
        recentOrders = List.copyOf(recentOrders);
    }
}

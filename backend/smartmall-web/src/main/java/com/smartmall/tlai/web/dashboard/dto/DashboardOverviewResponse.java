package com.smartmall.tlai.web.dashboard.dto;

import com.smartmall.tlai.business.dashboard.model.DashboardOverview;

import java.time.OffsetDateTime;
import java.util.List;

public record DashboardOverviewResponse(
        long weekOrders,
        double weekOrderChangePercent,
        long weekRefunds,
        double weekRefundChangePercent,
        long pendingTickets,
        double weekPaymentRate,
        double weekPaymentRateChangePoints,
        List<DashboardTrendResponse> trend,
        List<DashboardRecentOrderResponse> recentOrders,
        OffsetDateTime generatedAt
) {
    public static DashboardOverviewResponse from(DashboardOverview overview) {
        return new DashboardOverviewResponse(overview.weekOrders(), overview.weekOrderChangePercent(),
                overview.weekRefunds(), overview.weekRefundChangePercent(), overview.pendingTickets(),
                overview.weekPaymentRate(), overview.weekPaymentRateChangePoints(),
                overview.trend().stream().map(DashboardTrendResponse::from).toList(),
                overview.recentOrders().stream().map(DashboardRecentOrderResponse::from).toList(),
                overview.generatedAt());
    }
}

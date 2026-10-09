package com.smartmall.tlai.business.dashboard.model;

/** 仪表盘计算所需的当前周期与上期原始计数。 */
public record DashboardMetrics(
        long currentOrders,
        long previousOrders,
        long currentRefunds,
        long previousRefunds,
        long pendingTickets,
        long currentPaidOrders,
        long previousPaidOrders
) {
}

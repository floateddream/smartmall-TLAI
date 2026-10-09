package com.smartmall.tlai.web.dashboard.dto;

import com.smartmall.tlai.business.dashboard.model.DashboardTrendPoint;

import java.time.LocalDate;

public record DashboardTrendResponse(LocalDate date, long orderCount, long refundCount) {
    public static DashboardTrendResponse from(DashboardTrendPoint point) {
        return new DashboardTrendResponse(point.date(), point.orderCount(), point.refundCount());
    }
}

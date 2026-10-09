package com.smartmall.tlai.business.dashboard.model;

import java.time.LocalDate;

public record DashboardTrendPoint(LocalDate date, long orderCount, long refundCount) {
}

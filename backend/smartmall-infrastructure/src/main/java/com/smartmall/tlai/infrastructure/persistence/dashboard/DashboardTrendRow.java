package com.smartmall.tlai.infrastructure.persistence.dashboard;

import java.time.LocalDate;

public class DashboardTrendRow {
    private LocalDate date;
    private long orderCount;
    private long refundCount;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public long getOrderCount() { return orderCount; }
    public void setOrderCount(long orderCount) { this.orderCount = orderCount; }
    public long getRefundCount() { return refundCount; }
    public void setRefundCount(long refundCount) { this.refundCount = refundCount; }
}

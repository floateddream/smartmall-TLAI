package com.smartmall.tlai.infrastructure.persistence.dashboard;

public class DashboardMetricsRow {
    private long currentOrders;
    private long previousOrders;
    private long currentRefunds;
    private long previousRefunds;
    private long pendingTickets;
    private long currentPaidOrders;
    private long previousPaidOrders;

    public long getCurrentOrders() { return currentOrders; }
    public void setCurrentOrders(long currentOrders) { this.currentOrders = currentOrders; }
    public long getPreviousOrders() { return previousOrders; }
    public void setPreviousOrders(long previousOrders) { this.previousOrders = previousOrders; }
    public long getCurrentRefunds() { return currentRefunds; }
    public void setCurrentRefunds(long currentRefunds) { this.currentRefunds = currentRefunds; }
    public long getPreviousRefunds() { return previousRefunds; }
    public void setPreviousRefunds(long previousRefunds) { this.previousRefunds = previousRefunds; }
    public long getPendingTickets() { return pendingTickets; }
    public void setPendingTickets(long pendingTickets) { this.pendingTickets = pendingTickets; }
    public long getCurrentPaidOrders() { return currentPaidOrders; }
    public void setCurrentPaidOrders(long currentPaidOrders) { this.currentPaidOrders = currentPaidOrders; }
    public long getPreviousPaidOrders() { return previousPaidOrders; }
    public void setPreviousPaidOrders(long previousPaidOrders) { this.previousPaidOrders = previousPaidOrders; }
}

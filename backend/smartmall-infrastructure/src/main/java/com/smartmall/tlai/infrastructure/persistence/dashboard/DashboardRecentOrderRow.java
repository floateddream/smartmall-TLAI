package com.smartmall.tlai.infrastructure.persistence.dashboard;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class DashboardRecentOrderRow {
    private long id;
    private String orderNo;
    private String productSummary;
    private String customerName;
    private BigDecimal totalAmount;
    private String status;
    private OffsetDateTime createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getProductSummary() { return productSummary; }
    public void setProductSummary(String productSummary) { this.productSummary = productSummary; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}

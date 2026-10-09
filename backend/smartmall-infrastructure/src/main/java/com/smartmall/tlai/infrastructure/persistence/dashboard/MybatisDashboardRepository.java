package com.smartmall.tlai.infrastructure.persistence.dashboard;

import com.smartmall.tlai.business.dashboard.model.DashboardMetrics;
import com.smartmall.tlai.business.dashboard.model.DashboardRecentOrder;
import com.smartmall.tlai.business.dashboard.model.DashboardTrendPoint;
import com.smartmall.tlai.business.dashboard.repository.DashboardRepository;
import com.smartmall.tlai.business.order.domain.OrderStatus;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class MybatisDashboardRepository implements DashboardRepository {
    private final DashboardMapper mapper;

    public MybatisDashboardRepository(DashboardMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public DashboardMetrics findMetrics(OffsetDateTime currentStart, OffsetDateTime currentEnd,
                                        OffsetDateTime previousStart, OffsetDateTime previousEnd) {
        DashboardMetricsRow row = mapper.selectMetrics(currentStart, currentEnd, previousStart, previousEnd);
        return new DashboardMetrics(row.getCurrentOrders(), row.getPreviousOrders(), row.getCurrentRefunds(),
                row.getPreviousRefunds(), row.getPendingTickets(), row.getCurrentPaidOrders(),
                row.getPreviousPaidOrders());
    }

    @Override
    public List<DashboardTrendPoint> findTrend(OffsetDateTime start, OffsetDateTime end) {
        return mapper.selectTrend(start, end).stream()
                .map(row -> new DashboardTrendPoint(row.getDate(), row.getOrderCount(), row.getRefundCount()))
                .toList();
    }

    @Override
    public List<DashboardRecentOrder> findRecentOrders(int limit) {
        return mapper.selectRecentOrders(limit).stream()
                .map(row -> new DashboardRecentOrder(row.getId(), row.getOrderNo(), row.getProductSummary(),
                        row.getCustomerName(), row.getTotalAmount(), OrderStatus.valueOf(row.getStatus()),
                        row.getCreatedAt()))
                .toList();
    }
}

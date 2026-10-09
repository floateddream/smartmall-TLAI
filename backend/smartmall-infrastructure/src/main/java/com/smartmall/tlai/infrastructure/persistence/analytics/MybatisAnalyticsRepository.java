package com.smartmall.tlai.infrastructure.persistence.analytics;

import com.smartmall.tlai.analytics.model.AnalyticsDataPoint;
import com.smartmall.tlai.analytics.model.AnalyticsGranularity;
import com.smartmall.tlai.analytics.repository.AnalyticsRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class MybatisAnalyticsRepository implements AnalyticsRepository {
    private final AnalyticsMapper mapper;

    public MybatisAnalyticsRepository(AnalyticsMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<AnalyticsDataPoint> findOrderCountTrend(OffsetDateTime start, OffsetDateTime end,
                                                        AnalyticsGranularity granularity, int limit) {
        return points(mapper.selectOrderCountTrend(start, end, granularity.sqlValue(), limit));
    }

    @Override
    public List<AnalyticsDataPoint> findRefundAmountTrend(OffsetDateTime start, OffsetDateTime end,
                                                          AnalyticsGranularity granularity, int limit) {
        return points(mapper.selectRefundAmountTrend(start, end, granularity.sqlValue(), limit));
    }

    @Override
    public List<AnalyticsDataPoint> findRefundAmountByProduct(OffsetDateTime start, OffsetDateTime end, int limit) {
        return points(mapper.selectRefundAmountByProduct(start, end, limit));
    }

    @Override
    public List<AnalyticsDataPoint> findRefundCountByReason(OffsetDateTime start, OffsetDateTime end, int limit) {
        return points(mapper.selectRefundCountByReason(start, end, limit));
    }

    @Override
    public List<AnalyticsDataPoint> findTicketCountTrend(OffsetDateTime start, OffsetDateTime end,
                                                         AnalyticsGranularity granularity, int limit) {
        return points(mapper.selectTicketCountTrend(start, end, granularity.sqlValue(), limit));
    }

    @Override
    public List<AnalyticsDataPoint> findTicketCountByStatus(OffsetDateTime start, OffsetDateTime end, int limit) {
        return points(mapper.selectTicketCountByStatus(start, end, limit));
    }

    @Override
    public List<AnalyticsDataPoint> findTicketCountByCategory(OffsetDateTime start, OffsetDateTime end, int limit) {
        return points(mapper.selectTicketCountByCategory(start, end, limit));
    }

    private List<AnalyticsDataPoint> points(List<AnalyticsPointRow> rows) {
        return rows.stream().map(row -> new AnalyticsDataPoint(row.getLabel(), row.getMetricValue())).toList();
    }
}

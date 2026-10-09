package com.smartmall.tlai.analytics.application;

import com.smartmall.tlai.analytics.model.AnalyticsDataPoint;
import com.smartmall.tlai.analytics.model.AnalyticsDimension;
import com.smartmall.tlai.analytics.model.AnalyticsGranularity;
import com.smartmall.tlai.analytics.model.AnalyticsMetric;
import com.smartmall.tlai.analytics.model.AnalyticsQuery;
import com.smartmall.tlai.analytics.model.AnalyticsResult;
import com.smartmall.tlai.analytics.repository.AnalyticsRepository;
import com.smartmall.tlai.analytics.repository.AnalyticsResultCache;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsQueryServiceTest {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    @Mock AnalyticsRepository repository;
    @Mock AnalyticsResultCache cache;

    @Test
    void orderTrendUsesInclusiveShanghaiDateRange() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate startDate = today.minusDays(6);
        AnalyticsQuery query = new AnalyticsQuery(AnalyticsMetric.ORDER_COUNT, AnalyticsDimension.DATE,
                AnalyticsGranularity.DAY, startDate, today, 20);
        OffsetDateTime start = startDate.atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
        OffsetDateTime end = today.plusDays(1).atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
        List<AnalyticsDataPoint> points = List.of(new AnalyticsDataPoint(today.toString(), BigDecimal.TEN));
        when(repository.findOrderCountTrend(start, end, AnalyticsGranularity.DAY, 20)).thenReturn(points);

        var result = service().query(query);

        assertThat(result.query()).isEqualTo(query);
        assertThat(result.points()).isEqualTo(points);
        assertThat(result.cached()).isFalse();
        verify(repository).findOrderCountTrend(start, end, AnalyticsGranularity.DAY, 20);
        verify(cache).put(result);
    }

    @Test
    void refundProductRankingUsesFixedRepositoryMethod() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        AnalyticsQuery query = new AnalyticsQuery(AnalyticsMetric.REFUND_AMOUNT, AnalyticsDimension.PRODUCT,
                AnalyticsGranularity.MONTH, today.minusMonths(3), today, 10);
        OffsetDateTime start = query.startDate().atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
        OffsetDateTime end = today.plusDays(1).atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
        when(repository.findRefundAmountByProduct(start, end, 10)).thenReturn(List.of());

        service().query(query);

        verify(repository).findRefundAmountByProduct(start, end, 10);
        verify(repository, never()).findRefundAmountTrend(start, end, AnalyticsGranularity.MONTH, 10);
    }

    @Test
    void rejectsUnsupportedMetricAndDimensionCombination() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        AnalyticsQuery query = new AnalyticsQuery(AnalyticsMetric.ORDER_COUNT,
                AnalyticsDimension.REFUND_REASON, AnalyticsGranularity.DAY,
                today.minusDays(7), today, 20);

        assertInvalid(query, "不支持维度");
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsMoreThan366InclusiveDays() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        AnalyticsQuery query = new AnalyticsQuery(AnalyticsMetric.TICKET_COUNT, AnalyticsDimension.DATE,
                AnalyticsGranularity.MONTH, today.minusDays(366), today, 20);

        assertInvalid(query, "不能超过 366 天");
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsFutureDatesAndExcessiveLimit() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        assertInvalid(new AnalyticsQuery(AnalyticsMetric.TICKET_COUNT, AnalyticsDimension.TICKET_STATUS,
                AnalyticsGranularity.DAY, today, today.plusDays(1), 20), "不能晚于今天");
        assertInvalid(new AnalyticsQuery(AnalyticsMetric.TICKET_COUNT, AnalyticsDimension.TICKET_STATUS,
                AnalyticsGranularity.DAY, today, today, 51), "1 到 50");
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsTrendWhenTimeBucketsExceedResultLimit() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        AnalyticsQuery query = new AnalyticsQuery(AnalyticsMetric.ORDER_COUNT, AnalyticsDimension.DATE,
                AnalyticsGranularity.DAY, today.minusDays(30), today, 20);

        assertInvalid(query, "时间分组数量超过返回上限");
        verifyNoInteractions(repository);
    }

    @Test
    void returnsCachedResultWithoutQueryingDatabase() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        AnalyticsQuery query = new AnalyticsQuery(AnalyticsMetric.TICKET_COUNT,
                AnalyticsDimension.TICKET_STATUS, AnalyticsGranularity.MONTH,
                today.minusDays(30), today, 10);
        AnalyticsResult cached = new AnalyticsResult(query,
                List.of(new AnalyticsDataPoint("OPEN", BigDecimal.valueOf(8))), OffsetDateTime.now(), true);
        when(cache.find(query)).thenReturn(Optional.of(cached));

        AnalyticsResult result = service().query(query);

        assertThat(result).isEqualTo(cached);
        assertThat(result.cached()).isTrue();
        verifyNoInteractions(repository);
        verify(cache, never()).put(result);
    }

    private AnalyticsQueryService service() {
        return new AnalyticsQueryService(repository, cache);
    }

    private void assertInvalid(AnalyticsQuery query, String message) {
        assertThatThrownBy(() -> service().query(query))
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ANALYTICS_QUERY_INVALID);
                    assertThat(error.getMessage()).contains(message);
                });
    }
}

package com.smartmall.tlai.business.dashboard.application;

import com.smartmall.tlai.business.dashboard.model.DashboardMetrics;
import com.smartmall.tlai.business.dashboard.model.DashboardRecentOrder;
import com.smartmall.tlai.business.dashboard.model.DashboardTrendPoint;
import com.smartmall.tlai.business.dashboard.repository.DashboardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock DashboardRepository repository;

    @Test
    void overviewUsesShanghaiWeekBoundariesAndCalculatesRates() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-30T06:30:00Z"), ZoneOffset.UTC);
        when(repository.findMetrics(any(), any(), any(), any()))
                .thenReturn(new DashboardMetrics(120, 100, 10, 20, 8, 90, 80));
        when(repository.findTrend(any(), any())).thenReturn(List.of(
                new DashboardTrendPoint(LocalDate.of(2026, 9, 30), 18, 2)));
        when(repository.findRecentOrders(anyInt())).thenReturn(List.<DashboardRecentOrder>of());

        var result = new DashboardService(repository, clock).overview();

        assertThat(result.weekOrders()).isEqualTo(120);
        assertThat(result.weekOrderChangePercent()).isEqualTo(20.0);
        assertThat(result.weekRefundChangePercent()).isEqualTo(-50.0);
        assertThat(result.weekPaymentRate()).isEqualTo(75.0);
        assertThat(result.weekPaymentRateChangePoints()).isEqualTo(-5.0);
        assertThat(result.generatedAt()).isEqualTo(OffsetDateTime.parse("2026-09-30T14:30:00+08:00"));

        ArgumentCaptor<OffsetDateTime> currentStart = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> currentEnd = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> previousStart = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> previousEnd = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(repository).findMetrics(currentStart.capture(), currentEnd.capture(),
                previousStart.capture(), previousEnd.capture());
        assertThat(currentStart.getValue()).isEqualTo(OffsetDateTime.parse("2026-09-28T00:00:00+08:00"));
        assertThat(currentEnd.getValue()).isEqualTo(OffsetDateTime.parse("2026-09-30T14:30:00+08:00"));
        assertThat(previousStart.getValue()).isEqualTo(OffsetDateTime.parse("2026-09-21T00:00:00+08:00"));
        assertThat(previousEnd.getValue()).isEqualTo(OffsetDateTime.parse("2026-09-23T14:30:00+08:00"));

        verify(repository).findTrend(OffsetDateTime.parse("2026-09-24T00:00:00+08:00"),
                OffsetDateTime.parse("2026-10-01T00:00:00+08:00"));
        verify(repository).findRecentOrders(6);
    }

    @Test
    void overviewHandlesPeriodsWithoutOrders() {
        when(repository.findMetrics(any(), any(), any(), any()))
                .thenReturn(new DashboardMetrics(3, 0, 0, 0, 0, 0, 0));
        when(repository.findTrend(any(), any())).thenReturn(List.of());
        when(repository.findRecentOrders(anyInt())).thenReturn(List.of());

        var result = new DashboardService(repository, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)).overview();

        assertThat(result.weekOrderChangePercent()).isEqualTo(100.0);
        assertThat(result.weekRefundChangePercent()).isZero();
        assertThat(result.weekPaymentRate()).isZero();
        assertThat(result.weekPaymentRateChangePoints()).isZero();
    }
}

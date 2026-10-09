package com.smartmall.tlai.business.dashboard.application;

import com.smartmall.tlai.business.dashboard.model.DashboardMetrics;
import com.smartmall.tlai.business.dashboard.model.DashboardOverview;
import com.smartmall.tlai.business.dashboard.repository.DashboardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

@Service
public class DashboardService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int RECENT_ORDER_LIMIT = 6;

    private final DashboardRepository repository;
    private final Clock clock;

    @Autowired
    public DashboardService(DashboardRepository repository) {
        this(repository, Clock.systemUTC());
    }

    DashboardService(DashboardRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardOverview overview() {
        // 周环比只比较相同进度，例如周三下午只与上周三下午之前的数据比较。
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(BUSINESS_ZONE);
        LocalDate today = now.toLocalDate();
        LocalDate weekStartDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        ZonedDateTime weekStart = weekStartDate.atStartOfDay(BUSINESS_ZONE);
        ZonedDateTime previousStart = weekStart.minusWeeks(1);
        ZonedDateTime previousEnd = now.minusWeeks(1);
        DashboardMetrics metrics = repository.findMetrics(weekStart.toOffsetDateTime(), now.toOffsetDateTime(),
                previousStart.toOffsetDateTime(), previousEnd.toOffsetDateTime());

        ZonedDateTime trendStart = today.minusDays(6).atStartOfDay(BUSINESS_ZONE);
        ZonedDateTime trendEnd = today.plusDays(1).atStartOfDay(BUSINESS_ZONE);
        // 支付转化率以本周创建的订单为分母，已支付及进入退款流程的订单均视为支付成功。
        double currentPaymentRate = rate(metrics.currentPaidOrders(), metrics.currentOrders());
        double previousPaymentRate = rate(metrics.previousPaidOrders(), metrics.previousOrders());
        return new DashboardOverview(metrics.currentOrders(),
                changePercent(metrics.currentOrders(), metrics.previousOrders()),
                metrics.currentRefunds(), changePercent(metrics.currentRefunds(), metrics.previousRefunds()),
                metrics.pendingTickets(), currentPaymentRate,
                round(currentPaymentRate - previousPaymentRate),
                repository.findTrend(trendStart.toOffsetDateTime(), trendEnd.toOffsetDateTime()),
                repository.findRecentOrders(RECENT_ORDER_LIMIT), now.toOffsetDateTime());
    }

    private double changePercent(long current, long previous) {
        if (previous == 0) return current == 0 ? 0 : 100;
        return round((current - previous) * 100.0 / previous);
    }

    private double rate(long numerator, long denominator) {
        return denominator == 0 ? 0 : round(numerator * 100.0 / denominator);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}

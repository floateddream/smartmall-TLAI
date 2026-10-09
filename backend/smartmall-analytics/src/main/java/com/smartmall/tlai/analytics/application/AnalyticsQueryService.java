package com.smartmall.tlai.analytics.application;

import com.smartmall.tlai.analytics.model.AnalyticsDataPoint;
import com.smartmall.tlai.analytics.model.AnalyticsDimension;
import com.smartmall.tlai.analytics.model.AnalyticsMetric;
import com.smartmall.tlai.analytics.model.AnalyticsQuery;
import com.smartmall.tlai.analytics.model.AnalyticsResult;
import com.smartmall.tlai.analytics.repository.AnalyticsRepository;
import com.smartmall.tlai.analytics.repository.AnalyticsResultCache;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AnalyticsQueryService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int MAX_RANGE_DAYS = 366;
    private static final int MAX_RESULT_ROWS = 50;
    private static final Map<AnalyticsMetric, Set<AnalyticsDimension>> ALLOWED_COMBINATIONS = Map.of(
            AnalyticsMetric.ORDER_COUNT, Set.of(AnalyticsDimension.DATE),
            AnalyticsMetric.REFUND_AMOUNT, Set.of(AnalyticsDimension.DATE, AnalyticsDimension.PRODUCT),
            AnalyticsMetric.REFUND_COUNT, Set.of(AnalyticsDimension.REFUND_REASON),
            AnalyticsMetric.TICKET_COUNT, Set.of(AnalyticsDimension.DATE,
                    AnalyticsDimension.TICKET_STATUS, AnalyticsDimension.TICKET_CATEGORY)
    );

    private final AnalyticsRepository repository;
    private final AnalyticsResultCache cache;

    public AnalyticsQueryService(AnalyticsRepository repository, AnalyticsResultCache cache) {
        this.repository = repository;
        this.cache = cache;
    }

    @Transactional(readOnly = true)
    public AnalyticsResult query(AnalyticsQuery query) {
        validate(query);
        var cached = cache.find(query);
        if (cached.isPresent()) return cached.get();
        OffsetDateTime start = query.startDate().atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
        OffsetDateTime end = query.endDate().plusDays(1).atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();

        // 每个合法组合只映射到一个预定义仓库方法，模型或前端无法改变 SQL 结构。
        List<AnalyticsDataPoint> points = switch (query.metric()) {
            case ORDER_COUNT -> repository.findOrderCountTrend(start, end, query.granularity(), query.limit());
            case REFUND_AMOUNT -> query.dimension() == AnalyticsDimension.DATE
                    ? repository.findRefundAmountTrend(start, end, query.granularity(), query.limit())
                    : repository.findRefundAmountByProduct(start, end, query.limit());
            case REFUND_COUNT -> repository.findRefundCountByReason(start, end, query.limit());
            case TICKET_COUNT -> switch (query.dimension()) {
                case DATE -> repository.findTicketCountTrend(start, end, query.granularity(), query.limit());
                case TICKET_STATUS -> repository.findTicketCountByStatus(start, end, query.limit());
                case TICKET_CATEGORY -> repository.findTicketCountByCategory(start, end, query.limit());
                default -> throw invalid("工单指标不支持该分析维度");
            };
        };
        AnalyticsResult result = new AnalyticsResult(query, points, OffsetDateTime.now(BUSINESS_ZONE));
        cache.put(result);
        return result;
    }

    private void validate(AnalyticsQuery query) {
        if (query == null || query.metric() == null || query.dimension() == null
                || query.granularity() == null || query.startDate() == null || query.endDate() == null) {
            throw invalid("指标、维度、时间粒度和日期范围不能为空");
        }
        if (query.startDate().isAfter(query.endDate())) {
            throw invalid("开始日期不能晚于结束日期");
        }
        long days = ChronoUnit.DAYS.between(query.startDate(), query.endDate()) + 1;
        if (days > MAX_RANGE_DAYS) {
            throw invalid("查询时间范围不能超过 366 天");
        }
        if (query.endDate().isAfter(LocalDate.now(BUSINESS_ZONE))) {
            throw invalid("结束日期不能晚于今天");
        }
        if (query.limit() < 1 || query.limit() > MAX_RESULT_ROWS) {
            throw invalid("返回行数必须在 1 到 50 之间");
        }
        if (!ALLOWED_COMBINATIONS.getOrDefault(query.metric(), Set.of()).contains(query.dimension())) {
            throw invalid("指标 %s 不支持维度 %s".formatted(query.metric(), query.dimension()));
        }
        if (query.dimension() == AnalyticsDimension.DATE && bucketCount(query) > query.limit()) {
            throw invalid("时间分组数量超过返回上限，请提高 limit 或改用更大的时间粒度");
        }
    }

    private long bucketCount(AnalyticsQuery query) {
        return switch (query.granularity()) {
            case DAY -> ChronoUnit.DAYS.between(query.startDate(), query.endDate()) + 1;
            case WEEK -> ChronoUnit.WEEKS.between(
                    query.startDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    query.endDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))) + 1;
            case MONTH -> ChronoUnit.MONTHS.between(
                    YearMonth.from(query.startDate()), YearMonth.from(query.endDate())) + 1;
        };
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.ANALYTICS_QUERY_INVALID, message);
    }
}

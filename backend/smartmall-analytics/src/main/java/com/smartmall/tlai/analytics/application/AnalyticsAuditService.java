package com.smartmall.tlai.analytics.application;

import com.smartmall.tlai.analytics.model.AnalyticsAudit;
import com.smartmall.tlai.analytics.model.AnalyticsQuery;
import com.smartmall.tlai.analytics.model.AnalyticsResult;
import com.smartmall.tlai.analytics.repository.AnalyticsAuditRepository;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;

@Service
public class AnalyticsAuditService {
    private static final Set<String> QUERY_MODES = Set.of("STRUCTURED", "NATURAL_LANGUAGE");
    private static final Set<String> STATUSES = Set.of("SUCCESS", "FAILED");

    private final AnalyticsAuditRepository repository;

    public AnalyticsAuditService(AnalyticsAuditRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(long userId, String username, String queryMode, String question,
                              AnalyticsResult result, String modelName, long latencyMs) {
        AnalyticsQuery query = result.query();
        repository.insert(new AnalyticsAudit(null, userId, username, requireMode(queryMode), trimQuestion(question),
                query.metric(), query.dimension(), query.granularity(), query.startDate(), query.endDate(),
                query.limit(), result.points().size(), result.cached(), trimToNull(modelName), latencyMs,
                "SUCCESS", null, OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(long userId, String username, String queryMode, String question,
                              AnalyticsQuery query, String errorCode, long latencyMs) {
        repository.insert(new AnalyticsAudit(null, userId, username, requireMode(queryMode), trimQuestion(question),
                query == null ? null : query.metric(), query == null ? null : query.dimension(),
                query == null ? null : query.granularity(), query == null ? null : query.startDate(),
                query == null ? null : query.endDate(), query == null ? null : query.limit(),
                null, false, null, latencyMs, "FAILED", errorCode,
                OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Transactional(readOnly = true)
    public PageResponse<AnalyticsAudit> list(int page, int size, String queryMode, String status) {
        String normalizedMode = normalizeFilter(queryMode, QUERY_MODES, "查询模式");
        String normalizedStatus = normalizeFilter(status, STATUSES, "查询状态");
        return new PageResponse<>(repository.findPage(page, size, normalizedMode, normalizedStatus),
                repository.count(normalizedMode, normalizedStatus), page, size);
    }

    private static String requireMode(String value) {
        String normalized = value == null ? "" : value.toUpperCase(Locale.ROOT);
        if (!QUERY_MODES.contains(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的分析查询模式");
        }
        return normalized;
    }

    private static String normalizeFilter(String value, Set<String> allowed, String label) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + "不合法");
        }
        return normalized;
    }

    private static String trimQuestion(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

package com.smartmall.tlai.analytics.application;

import com.smartmall.tlai.analytics.model.AnalyticsAudit;
import com.smartmall.tlai.analytics.model.AnalyticsDimension;
import com.smartmall.tlai.analytics.model.AnalyticsGranularity;
import com.smartmall.tlai.analytics.model.AnalyticsMetric;
import com.smartmall.tlai.analytics.model.AnalyticsDataPoint;
import com.smartmall.tlai.analytics.model.AnalyticsQuery;
import com.smartmall.tlai.analytics.model.AnalyticsResult;
import com.smartmall.tlai.analytics.repository.AnalyticsAuditRepository;
import com.smartmall.tlai.common.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsAuditServiceTest {
    @Mock
    private AnalyticsAuditRepository repository;

    @Test
    void shouldRecordSuccessfulCachedQuery() {
        AnalyticsAuditService service = new AnalyticsAuditService(repository);
        AnalyticsQuery query = new AnalyticsQuery(AnalyticsMetric.ORDER_COUNT, AnalyticsDimension.DATE,
                AnalyticsGranularity.DAY, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2), 10);
        AnalyticsResult result = new AnalyticsResult(query,
                List.of(new AnalyticsDataPoint("2026-09-01", BigDecimal.valueOf(3))), OffsetDateTime.now(), true);

        service.recordSuccess(7L, "admin", "structured", null, result, null, 18L);

        ArgumentCaptor<AnalyticsAudit> captor = ArgumentCaptor.forClass(AnalyticsAudit.class);
        verify(repository).insert(captor.capture());
        AnalyticsAudit audit = captor.getValue();
        assertEquals("STRUCTURED", audit.queryMode());
        assertEquals("SUCCESS", audit.status());
        assertEquals(1, audit.resultCount());
        assertTrue(audit.cacheHit());
        assertEquals(AnalyticsMetric.ORDER_COUNT, audit.metric());
    }

    @Test
    void shouldRejectUnsupportedListFilter() {
        AnalyticsAuditService service = new AnalyticsAuditService(repository);

        assertThrows(BusinessException.class, () -> service.list(1, 20, "SQL", null));
    }

    @Test
    void shouldReturnRepositoryPage() {
        AnalyticsAuditService service = new AnalyticsAuditService(repository);
        AnalyticsAudit audit = new AnalyticsAudit(1L, 7L, "admin", "NATURAL_LANGUAGE", "订单趋势",
                AnalyticsMetric.ORDER_COUNT, AnalyticsDimension.DATE, AnalyticsGranularity.DAY,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2), 10, 2, false,
                "deepseek-chat", 120L, "SUCCESS", null, OffsetDateTime.now());
        when(repository.findPage(2, 10, "NATURAL_LANGUAGE", "SUCCESS")).thenReturn(List.of(audit));
        when(repository.count("NATURAL_LANGUAGE", "SUCCESS")).thenReturn(11L);

        var page = service.list(2, 10, "natural_language", "success");

        assertEquals(List.of(audit), page.items());
        assertEquals(11L, page.total());
        assertEquals(2, page.page());
        assertEquals(10, page.size());
    }
}

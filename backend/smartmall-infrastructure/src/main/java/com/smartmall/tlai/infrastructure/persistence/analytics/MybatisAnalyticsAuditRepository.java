package com.smartmall.tlai.infrastructure.persistence.analytics;

import com.smartmall.tlai.analytics.model.AnalyticsAudit;
import com.smartmall.tlai.analytics.model.AnalyticsDimension;
import com.smartmall.tlai.analytics.model.AnalyticsGranularity;
import com.smartmall.tlai.analytics.model.AnalyticsMetric;
import com.smartmall.tlai.analytics.repository.AnalyticsAuditRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MybatisAnalyticsAuditRepository implements AnalyticsAuditRepository {
    private final AnalyticsAuditMapper mapper;

    public MybatisAnalyticsAuditRepository(AnalyticsAuditMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void insert(AnalyticsAudit audit) {
        mapper.insert(toEntity(audit));
    }

    @Override
    public long count(String queryMode, String status) {
        return mapper.countAudits(queryMode, status);
    }

    @Override
    public List<AnalyticsAudit> findPage(int page, int size, String queryMode, String status) {
        long offset = ((long) page - 1) * size;
        return mapper.selectPage(offset, size, queryMode, status).stream().map(this::toDomain).toList();
    }

    private AnalyticsAudit toDomain(AnalyticsAuditPersistenceEntity entity) {
        return new AnalyticsAudit(entity.getId(), entity.getUserId(), entity.getUsername(), entity.getQueryMode(),
                entity.getQuestion(), enumValue(AnalyticsMetric.class, entity.getMetric()),
                enumValue(AnalyticsDimension.class, entity.getDimension()),
                enumValue(AnalyticsGranularity.class, entity.getGranularity()), entity.getStartDate(),
                entity.getEndDate(), entity.getResultLimit(), entity.getResultCount(),
                Boolean.TRUE.equals(entity.getCacheHit()), entity.getModelName(), entity.getLatencyMs(),
                entity.getStatus(), entity.getErrorCode(), entity.getCreatedAt());
    }

    private AnalyticsAuditPersistenceEntity toEntity(AnalyticsAudit audit) {
        AnalyticsAuditPersistenceEntity entity = new AnalyticsAuditPersistenceEntity();
        entity.setId(audit.id());
        entity.setUserId(audit.userId());
        entity.setUsername(audit.username());
        entity.setQueryMode(audit.queryMode());
        entity.setQuestion(audit.question());
        entity.setMetric(audit.metric() == null ? null : audit.metric().name());
        entity.setDimension(audit.dimension() == null ? null : audit.dimension().name());
        entity.setGranularity(audit.granularity() == null ? null : audit.granularity().name());
        entity.setStartDate(audit.startDate());
        entity.setEndDate(audit.endDate());
        entity.setResultLimit(audit.resultLimit());
        entity.setResultCount(audit.resultCount());
        entity.setCacheHit(audit.cacheHit());
        entity.setModelName(audit.modelName());
        entity.setLatencyMs(audit.latencyMs());
        entity.setStatus(audit.status());
        entity.setErrorCode(audit.errorCode());
        entity.setCreatedAt(audit.createdAt());
        return entity;
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }
}

package com.smartmall.tlai.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmall.tlai.analytics.model.AnalyticsDataPoint;
import com.smartmall.tlai.analytics.model.AnalyticsQuery;
import com.smartmall.tlai.analytics.model.AnalyticsResult;
import com.smartmall.tlai.analytics.repository.AnalyticsResultCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class RedisAnalyticsResultCache implements AnalyticsResultCache {
    private static final Logger log = LoggerFactory.getLogger(RedisAnalyticsResultCache.class);
    private static final String KEY_PREFIX = "smartmall:analytics:v1:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RedisAnalyticsResultCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                                     @Value("${smartmall.analytics.cache-ttl:5m}") Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    @Override
    public Optional<AnalyticsResult> find(AnalyticsQuery query) {
        try {
            String value = redisTemplate.opsForValue().get(key(query));
            if (value == null) return Optional.empty();
            CachePayload payload = objectMapper.readValue(value, CachePayload.class);
            return Optional.of(new AnalyticsResult(query, payload.points(), payload.generatedAt(), true));
        } catch (DataAccessException | JsonProcessingException exception) {
            // 缓存故障只能降低性能，不能阻断真实数据查询。
            log.warn("Analytics cache read failed, falling back to PostgreSQL: type={}",
                    exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public void put(AnalyticsResult result) {
        try {
            String value = objectMapper.writeValueAsString(
                    new CachePayload(result.points(), result.generatedAt()));
            redisTemplate.opsForValue().set(key(result.query()), value, ttl);
        } catch (DataAccessException | JsonProcessingException exception) {
            log.warn("Analytics cache write failed, result will not be cached: type={}",
                    exception.getClass().getSimpleName());
        }
    }

    private static String key(AnalyticsQuery query) {
        return KEY_PREFIX + String.join(":", query.metric().name(), query.dimension().name(),
                query.granularity().name(), query.startDate().toString(), query.endDate().toString(),
                Integer.toString(query.limit()));
    }

    private record CachePayload(List<AnalyticsDataPoint> points, OffsetDateTime generatedAt) {
    }
}

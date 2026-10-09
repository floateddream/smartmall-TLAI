package com.smartmall.tlai.infrastructure.persistence.analytics;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface AnalyticsMapper {
    @Select("""
            WITH aggregated AS (
                SELECT date_trunc(#{bucket}, created_at AT TIME ZONE 'Asia/Shanghai') AS bucket_time,
                       COUNT(*)::numeric AS metric_value
                FROM orders
                WHERE created_at >= #{start} AND created_at < #{end}
                GROUP BY 1
            )
            SELECT CASE WHEN #{bucket} = 'month' THEN TO_CHAR(bucket_time, 'YYYY-MM')
                        ELSE TO_CHAR(bucket_time, 'YYYY-MM-DD') END AS label,
                   metric_value
            FROM aggregated
            ORDER BY bucket_time
            LIMIT #{limit}
            """)
    List<AnalyticsPointRow> selectOrderCountTrend(@Param("start") OffsetDateTime start,
                                                   @Param("end") OffsetDateTime end,
                                                   @Param("bucket") String bucket,
                                                   @Param("limit") int limit);

    @Select("""
            WITH aggregated AS (
                SELECT date_trunc(#{bucket}, COALESCE(reviewed_at, applied_at) AT TIME ZONE 'Asia/Shanghai') AS bucket_time,
                       COALESCE(SUM(amount), 0)::numeric AS metric_value
                FROM refund_request
                WHERE status = 'COMPLETED'
                  AND COALESCE(reviewed_at, applied_at) >= #{start}
                  AND COALESCE(reviewed_at, applied_at) < #{end}
                GROUP BY 1
            )
            SELECT CASE WHEN #{bucket} = 'month' THEN TO_CHAR(bucket_time, 'YYYY-MM')
                        ELSE TO_CHAR(bucket_time, 'YYYY-MM-DD') END AS label,
                   metric_value
            FROM aggregated
            ORDER BY bucket_time
            LIMIT #{limit}
            """)
    List<AnalyticsPointRow> selectRefundAmountTrend(@Param("start") OffsetDateTime start,
                                                     @Param("end") OffsetDateTime end,
                                                     @Param("bucket") String bucket,
                                                     @Param("limit") int limit);

    @Select("""
            SELECT oi.product_name AS label, COALESCE(SUM(oi.subtotal), 0)::numeric AS metric_value
            FROM refund_request refund
            JOIN order_item oi ON oi.order_id = refund.order_id
            WHERE refund.status = 'COMPLETED'
              AND COALESCE(refund.reviewed_at, refund.applied_at) >= #{start}
              AND COALESCE(refund.reviewed_at, refund.applied_at) < #{end}
            GROUP BY oi.product_id, oi.product_name
            ORDER BY metric_value DESC, oi.product_name
            LIMIT #{limit}
            """)
    List<AnalyticsPointRow> selectRefundAmountByProduct(@Param("start") OffsetDateTime start,
                                                         @Param("end") OffsetDateTime end,
                                                         @Param("limit") int limit);

    @Select("""
            SELECT reason AS label, COUNT(*)::numeric AS metric_value
            FROM refund_request
            WHERE applied_at >= #{start} AND applied_at < #{end}
            GROUP BY reason
            ORDER BY metric_value DESC, reason
            LIMIT #{limit}
            """)
    List<AnalyticsPointRow> selectRefundCountByReason(@Param("start") OffsetDateTime start,
                                                       @Param("end") OffsetDateTime end,
                                                       @Param("limit") int limit);

    @Select("""
            WITH aggregated AS (
                SELECT date_trunc(#{bucket}, created_at AT TIME ZONE 'Asia/Shanghai') AS bucket_time,
                       COUNT(*)::numeric AS metric_value
                FROM after_sales_ticket
                WHERE created_at >= #{start} AND created_at < #{end}
                GROUP BY 1
            )
            SELECT CASE WHEN #{bucket} = 'month' THEN TO_CHAR(bucket_time, 'YYYY-MM')
                        ELSE TO_CHAR(bucket_time, 'YYYY-MM-DD') END AS label,
                   metric_value
            FROM aggregated
            ORDER BY bucket_time
            LIMIT #{limit}
            """)
    List<AnalyticsPointRow> selectTicketCountTrend(@Param("start") OffsetDateTime start,
                                                    @Param("end") OffsetDateTime end,
                                                    @Param("bucket") String bucket,
                                                    @Param("limit") int limit);

    @Select("""
            SELECT status AS label, COUNT(*)::numeric AS metric_value
            FROM after_sales_ticket
            WHERE created_at >= #{start} AND created_at < #{end}
            GROUP BY status
            ORDER BY metric_value DESC, status
            LIMIT #{limit}
            """)
    List<AnalyticsPointRow> selectTicketCountByStatus(@Param("start") OffsetDateTime start,
                                                       @Param("end") OffsetDateTime end,
                                                       @Param("limit") int limit);

    @Select("""
            SELECT category AS label, COUNT(*)::numeric AS metric_value
            FROM after_sales_ticket
            WHERE created_at >= #{start} AND created_at < #{end}
            GROUP BY category
            ORDER BY metric_value DESC, category
            LIMIT #{limit}
            """)
    List<AnalyticsPointRow> selectTicketCountByCategory(@Param("start") OffsetDateTime start,
                                                         @Param("end") OffsetDateTime end,
                                                         @Param("limit") int limit);
}

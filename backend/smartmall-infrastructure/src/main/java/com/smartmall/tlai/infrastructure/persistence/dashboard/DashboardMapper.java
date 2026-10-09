package com.smartmall.tlai.infrastructure.persistence.dashboard;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface DashboardMapper {
    @Select("""
            SELECT
                COUNT(*) FILTER (WHERE created_at >= #{currentStart} AND created_at < #{currentEnd}) AS current_orders,
                COUNT(*) FILTER (WHERE created_at >= #{previousStart} AND created_at < #{previousEnd}) AS previous_orders,
                COUNT(*) FILTER (WHERE created_at >= #{currentStart} AND created_at < #{currentEnd}
                    AND status IN ('PAID', 'REFUNDING', 'REFUNDED')) AS current_paid_orders,
                COUNT(*) FILTER (WHERE created_at >= #{previousStart} AND created_at < #{previousEnd}
                    AND status IN ('PAID', 'REFUNDING', 'REFUNDED')) AS previous_paid_orders,
                (SELECT COUNT(*) FROM refund_request
                    WHERE applied_at >= #{currentStart} AND applied_at < #{currentEnd}) AS current_refunds,
                (SELECT COUNT(*) FROM refund_request
                    WHERE applied_at >= #{previousStart} AND applied_at < #{previousEnd}) AS previous_refunds,
                (SELECT COUNT(*) FROM after_sales_ticket
                    WHERE status IN ('OPEN', 'PROCESSING')) AS pending_tickets
            FROM orders
            """)
    DashboardMetricsRow selectMetrics(@Param("currentStart") OffsetDateTime currentStart,
                                      @Param("currentEnd") OffsetDateTime currentEnd,
                                      @Param("previousStart") OffsetDateTime previousStart,
                                      @Param("previousEnd") OffsetDateTime previousEnd);

    @Select("""
            WITH days AS (
                SELECT generate_series(
                    CAST(#{start} AS TIMESTAMP WITH TIME ZONE),
                    CAST(#{end} AS TIMESTAMP WITH TIME ZONE) - INTERVAL '1 day',
                    INTERVAL '1 day'
                ) AS day_start
            )
            SELECT
                (day_start AT TIME ZONE 'Asia/Shanghai')::date AS date,
                (SELECT COUNT(*) FROM orders
                    WHERE created_at >= day_start AND created_at < day_start + INTERVAL '1 day') AS order_count,
                (SELECT COUNT(*) FROM refund_request
                    WHERE applied_at >= day_start AND applied_at < day_start + INTERVAL '1 day') AS refund_count
            FROM days
            ORDER BY day_start
            """)
    List<DashboardTrendRow> selectTrend(@Param("start") OffsetDateTime start,
                                        @Param("end") OffsetDateTime end);

    @Select("""
            SELECT o.id, o.order_no, o.customer_name, o.total_amount, o.status, o.created_at,
                CASE
                    WHEN items.first_product_name IS NULL THEN '暂无商品'
                    WHEN items.item_count > 1 THEN items.first_product_name || ' 等 ' || items.item_count || ' 件'
                    ELSE items.first_product_name
                END AS product_summary
            FROM orders o
            LEFT JOIN LATERAL (
                SELECT (ARRAY_AGG(oi.product_name ORDER BY oi.id))[1] AS first_product_name,
                       COALESCE(SUM(oi.quantity), 0) AS item_count
                FROM order_item oi
                WHERE oi.order_id = o.id
            ) items ON TRUE
            ORDER BY o.created_at DESC, o.id DESC
            LIMIT #{limit}
            """)
    List<DashboardRecentOrderRow> selectRecentOrders(@Param("limit") int limit);
}

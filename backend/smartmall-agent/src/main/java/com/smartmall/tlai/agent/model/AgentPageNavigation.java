package com.smartmall.tlai.agent.model;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 后端生成的业务页面导航；路径和查询参数均按页面白名单收敛。 */
public record AgentPageNavigation(String path, Map<String, String> query) {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Map<String, Set<String>> ALLOWED_QUERY_KEYS = Map.of(
            "/orders", Set.of("id", "keyword", "status"),
            "/refunds", Set.of("id", "keyword", "status"),
            "/tickets", Set.of("id", "keyword", "status", "category"),
            "/knowledge", Set.of("keyword", "status"),
            "/data-analysis", Set.of("report", "startDate", "endDate", "granularity", "limit")
    );
    private static final Set<String> ORDER_STATUSES = Set.of(
            "PENDING_PAYMENT", "PAID", "CANCELLED", "REFUNDING", "REFUNDED");
    private static final Set<String> REFUND_STATUSES = Set.of("PENDING_REVIEW", "COMPLETED", "REJECTED");
    private static final Set<String> TICKET_STATUSES = Set.of("OPEN", "PROCESSING", "RESOLVED", "CLOSED");
    private static final Set<String> TICKET_CATEGORIES = Set.of(
            "RETURN_EXCHANGE", "LOGISTICS", "PRODUCT_QUALITY", "OTHER");
    private static final Set<String> KNOWLEDGE_STATUSES = Set.of("UPLOADED", "PROCESSING", "READY", "FAILED");
    private static final Set<String> REPORTS = Set.of(
            "ORDER_TREND", "REFUND_TREND", "REFUND_PRODUCT", "REFUND_REASON",
            "TICKET_TREND", "TICKET_STATUS", "TICKET_CATEGORY");

    public AgentPageNavigation {
        if (!ALLOWED_QUERY_KEYS.containsKey(path)) {
            throw new IllegalArgumentException("不支持的 Agent 页面导航");
        }
        Map<String, String> source = query == null ? Map.of() : query;
        Map<String, String> sanitized = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (ALLOWED_QUERY_KEYS.get(path).contains(key) && validValue(path, key, value)) {
                sanitized.put(key, value.trim());
            }
        });
        if ("/data-analysis".equals(path)) sanitizeDateRange(sanitized);
        query = Map.copyOf(sanitized);
    }

    private static boolean validValue(String path, String key, String value) {
        if (value == null || value.isBlank() || containsControlCharacter(value)) return false;
        String normalized = value.trim();
        return switch (key) {
            case "id" -> normalized.matches("[1-9]\\d{0,15}");
            case "keyword" -> normalized.length() <= 100;
            case "status" -> switch (path) {
                case "/orders" -> ORDER_STATUSES.contains(normalized);
                case "/refunds" -> REFUND_STATUSES.contains(normalized);
                case "/tickets" -> TICKET_STATUSES.contains(normalized);
                case "/knowledge" -> KNOWLEDGE_STATUSES.contains(normalized);
                default -> false;
            };
            case "category" -> TICKET_CATEGORIES.contains(normalized);
            case "report" -> REPORTS.contains(normalized);
            case "granularity" -> Set.of("DAY", "WEEK", "MONTH").contains(normalized);
            case "startDate", "endDate" -> validDate(normalized);
            case "limit" -> validLimit(normalized);
            default -> false;
        };
    }

    private static void sanitizeDateRange(Map<String, String> query) {
        String startValue = query.get("startDate");
        String endValue = query.get("endDate");
        if (startValue == null || endValue == null) {
            query.remove("startDate");
            query.remove("endDate");
            return;
        }
        LocalDate start = LocalDate.parse(startValue);
        LocalDate end = LocalDate.parse(endValue);
        if (start.isAfter(end) || end.isAfter(LocalDate.now(BUSINESS_ZONE))
                || ChronoUnit.DAYS.between(start, end) + 1 > 366) {
            query.remove("startDate");
            query.remove("endDate");
        }
    }

    private static boolean validDate(String value) {
        try {
            return value.matches("\\d{4}-\\d{2}-\\d{2}") && LocalDate.parse(value) != null;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static boolean validLimit(String value) {
        try {
            int limit = Integer.parseInt(value);
            return limit >= 1 && limit <= 50;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static boolean containsControlCharacter(String value) {
        return value.chars().anyMatch(character -> character < 32 || character == 127);
    }
}

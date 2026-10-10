package com.smartmall.tlai.agent.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartmall.tlai.agent.model.AgentPageNavigation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 把工具参数转换为固定业务路由，模型无法提供或覆盖目标路径。 */
final class AgentToolNavigationFactory {
    private static final Set<String> ORDER_STATUSES = Set.of(
            "PENDING_PAYMENT", "PAID", "CANCELLED", "REFUNDING", "REFUNDED");
    private static final Set<String> REFUND_STATUSES = Set.of("PENDING_REVIEW", "COMPLETED", "REJECTED");
    private static final Set<String> TICKET_STATUSES = Set.of("OPEN", "PROCESSING", "RESOLVED", "CLOSED");
    private static final Set<String> TICKET_CATEGORIES = Set.of(
            "RETURN_EXCHANGE", "LOGISTICS", "PRODUCT_QUALITY", "OTHER");
    private static final Set<String> REPORTS = Set.of(
            "ORDER_TREND", "REFUND_TREND", "REFUND_PRODUCT", "REFUND_REASON",
            "TICKET_TREND", "TICKET_STATUS", "TICKET_CATEGORY");
    private static final Set<String> GRANULARITIES = Set.of("DAY", "WEEK", "MONTH");

    private AgentToolNavigationFactory() {
    }

    static AgentPageNavigation create(String toolName, JsonNode arguments) {
        if (arguments == null || !arguments.isObject()) return null;
        return switch (toolName) {
            case "queryOrder" -> new AgentPageNavigation("/orders",
                    listQuery(arguments, ORDER_STATUSES, null));
            case "queryRefund" -> new AgentPageNavigation("/refunds",
                    listQuery(arguments, REFUND_STATUSES, null));
            case "queryTicket" -> new AgentPageNavigation("/tickets",
                    listQuery(arguments, TICKET_STATUSES, TICKET_CATEGORIES));
            case "knowledgeBaseSearch" -> new AgentPageNavigation("/knowledge",
                    mapOfPresent("keyword", text(arguments, "query", 100)));
            case "topRefundProducts" -> analytics(arguments, "REFUND_PRODUCT", "MONTH");
            case "refundReasonDistribution" -> analytics(arguments, "REFUND_REASON", "MONTH");
            case "analyticsQuery" -> analytics(arguments, enumText(arguments, "report", REPORTS),
                    enumText(arguments, "granularity", GRANULARITIES));
            default -> null;
        };
    }

    private static AgentPageNavigation analytics(JsonNode arguments, String report, String granularity) {
        Map<String, String> query = new LinkedHashMap<>();
        put(query, "report", report);
        put(query, "startDate", text(arguments, "startDate", 10));
        put(query, "endDate", text(arguments, "endDate", 10));
        put(query, "granularity", granularity);
        Integer limit = integer(arguments, "limit", 1, 50);
        if (limit != null) query.put("limit", Integer.toString(limit));
        return new AgentPageNavigation("/data-analysis", query);
    }

    private static Map<String, String> listQuery(JsonNode arguments, Set<String> statuses,
                                                  Set<String> categories) {
        Map<String, String> query = new LinkedHashMap<>();
        Long id = positiveId(arguments, "id");
        if (id != null) query.put("id", Long.toString(id));
        put(query, "keyword", text(arguments, "keyword", 100));
        put(query, "status", enumText(arguments, "status", statuses));
        if (categories != null) put(query, "category", enumText(arguments, "category", categories));
        return query;
    }

    private static Map<String, String> mapOfPresent(String key, String value) {
        return value == null ? Map.of() : Map.of(key, value);
    }

    private static void put(Map<String, String> target, String key, String value) {
        if (value != null) target.put(key, value);
    }

    private static String enumText(JsonNode arguments, String field, Set<String> allowed) {
        String value = text(arguments, field, 40);
        return value != null && allowed.contains(value) ? value : null;
    }

    private static String text(JsonNode arguments, String field, int maxLength) {
        JsonNode value = arguments.get(field);
        if (value == null || !value.isTextual()) return null;
        String normalized = value.textValue().trim();
        if (normalized.isEmpty() || normalized.length() > maxLength
                || normalized.chars().anyMatch(character -> character < 32 || character == 127)) return null;
        return normalized;
    }

    private static Long positiveId(JsonNode arguments, String field) {
        JsonNode value = arguments.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) return null;
        long id = value.longValue();
        return id > 0 && id <= 9_007_199_254_740_991L ? id : null;
    }

    private static Integer integer(JsonNode arguments, String field, int min, int max) {
        JsonNode value = arguments.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) return null;
        int number = value.intValue();
        return number >= min && number <= max ? number : null;
    }
}

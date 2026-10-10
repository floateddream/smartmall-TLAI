package com.smartmall.tlai.agent.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartmall.tlai.agent.model.AgentActionConfirmation;
import com.smartmall.tlai.agent.model.AgentActionExecution;
import com.smartmall.tlai.agent.model.AgentActionStatus;
import com.smartmall.tlai.agent.model.AgentCapability;
import com.smartmall.tlai.agent.model.AgentCapabilityType;
import com.smartmall.tlai.agent.model.AgentPendingAction;
import com.smartmall.tlai.agent.model.AgentUserContext;
import com.smartmall.tlai.agent.repository.AgentActionConfirmationRepository;
import com.smartmall.tlai.business.order.application.OrderService;
import com.smartmall.tlai.business.ticket.application.AddTicketMessageCommand;
import com.smartmall.tlai.business.ticket.application.CreateTicketCommand;
import com.smartmall.tlai.business.ticket.application.TicketService;
import com.smartmall.tlai.business.ticket.domain.TicketCategory;
import com.smartmall.tlai.business.ticket.domain.TicketPriority;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;

@Service
public class PendingAgentActionService {
    private static final Logger log = LoggerFactory.getLogger(PendingAgentActionService.class);
    private static final Duration CONFIRMATION_TTL = Duration.ofMinutes(10);
    private static final int MAX_TOKEN_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final AgentActionConfirmationRepository repository;
    private final OrderService orderService;
    private final TicketService ticketService;
    private final AgentToolAuditService auditService;
    private final AgentCapabilityRegistry capabilityRegistry;
    private final ObjectMapper objectMapper;

    public PendingAgentActionService(AgentActionConfirmationRepository repository, OrderService orderService,
                                     TicketService ticketService, AgentToolAuditService auditService,
                                     AgentCapabilityRegistry capabilityRegistry, ObjectMapper objectMapper) {
        this.repository = repository;
        this.orderService = orderService;
        this.ticketService = ticketService;
        this.auditService = auditService;
        this.capabilityRegistry = capabilityRegistry;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AgentPendingAction create(String actionName, JsonNode arguments, AgentUserContext user) {
        AgentCapability definition = requireDefinition(actionName);
        requirePermission(user, definition.permission());
        NormalizedAction normalized = normalize(actionName, arguments);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String token = createToken();
        repository.insert(new AgentActionConfirmation(null, hashToken(token), user.userId(), user.username(),
                actionName, toJson(normalized.arguments()), AgentActionStatus.PENDING,
                now.plus(CONFIRMATION_TTL), null, now, null));
        log.info("Agent 操作等待用户确认：操作={}，用户ID={}，过期时间={}",
                actionName, user.userId(), now.plus(CONFIRMATION_TTL));
        return new AgentPendingAction(token, actionName, definition.displayName(),
                normalized.summary(), now.plus(CONFIRMATION_TTL));
    }

    @Transactional
    public AgentActionExecution confirm(String token, AgentUserContext user) {
        long startedAt = System.nanoTime();
        AgentActionConfirmation confirmation = requireOwned(token, user);
        AgentCapability definition = requireDefinition(confirmation.actionName());
        if (confirmation.status() == AgentActionStatus.SUCCEEDED) {
            return completedResult(confirmation, definition, elapsedMs(startedAt));
        }
        rejectUnavailableState(confirmation.status());

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (!confirmation.expiresAt().isAfter(now)) {
            repository.markExpired(confirmation.id());
            throw new BusinessException(ErrorCode.AGENT_ACTION_EXPIRED);
        }
        requirePermission(user, definition.permission());

        if (!repository.markExecuting(confirmation.id(), user.userId(), now)) {
            AgentActionConfirmation current = repository.findByTokenHash(confirmation.tokenHash())
                    .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_ACTION_NOT_FOUND));
            if (current.status() == AgentActionStatus.SUCCEEDED) {
                return completedResult(current, definition, elapsedMs(startedAt));
            }
            rejectUnavailableState(current.status());
            throw new BusinessException(ErrorCode.AGENT_ACTION_IN_PROGRESS);
        }

        try {
            String summary = execute(confirmation, user);
            repository.markSucceeded(confirmation.id(), summary);
            long latencyMs = elapsedMs(startedAt);
            safeAudit(user, confirmation.actionName(), confirmation.argumentsJson(),
                    "SUCCESS", latencyMs, null);
            log.info("Agent 操作执行成功：操作={}，用户ID={}，耗时毫秒={}",
                    confirmation.actionName(), user.userId(), latencyMs);
            return new AgentActionExecution(confirmation.actionName(), definition.displayName(), summary,
                    AgentActionStatus.SUCCEEDED, latencyMs);
        } catch (RuntimeException exception) {
            long latencyMs = elapsedMs(startedAt);
            String errorCode = exception instanceof BusinessException business
                    ? business.getErrorCode().code() : ErrorCode.INTERNAL_ERROR.code();
            safeAudit(user, confirmation.actionName(), confirmation.argumentsJson(),
                    "FAILED", latencyMs, errorCode);
            log.warn("Agent 操作执行失败：操作={}，用户ID={}，错误码={}",
                    confirmation.actionName(), user.userId(), errorCode);
            throw exception;
        }
    }

    @Transactional
    public AgentActionExecution cancel(String token, AgentUserContext user) {
        AgentActionConfirmation confirmation = requireOwned(token, user);
        AgentCapability definition = requireDefinition(confirmation.actionName());
        if (confirmation.status() == AgentActionStatus.CANCELLED) {
            return cancelledResult(confirmation, definition);
        }
        if (confirmation.status() != AgentActionStatus.PENDING) {
            rejectUnavailableState(confirmation.status());
        }
        if (!confirmation.expiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            repository.markExpired(confirmation.id());
            throw new BusinessException(ErrorCode.AGENT_ACTION_EXPIRED);
        }
        if (!repository.cancel(confirmation.id(), user.userId())) {
            throw new BusinessException(ErrorCode.AGENT_ACTION_IN_PROGRESS);
        }
        log.info("Agent 操作已取消：操作={}，用户ID={}", confirmation.actionName(), user.userId());
        return cancelledResult(confirmation, definition);
    }

    private String execute(AgentActionConfirmation confirmation, AgentUserContext user) {
        JsonNode arguments = parseArguments(confirmation.argumentsJson());
        return switch (confirmation.actionName()) {
            case "createAfterSalesTicket" -> {
                NormalizedAction normalized = normalizeCreateTicket(arguments);
                JsonNode values = normalized.arguments();
                long orderId = values.get("orderId").isNull()
                        ? orderService.getByOrderNo(values.get("orderNo").textValue()).id()
                        : values.get("orderId").longValue();
                var ticket = ticketService.create(new CreateTicketCommand(
                        orderId,
                        TicketCategory.valueOf(values.get("category").textValue()),
                        TicketPriority.valueOf(values.get("priority").textValue()),
                        values.get("subject").textValue(), values.get("description").textValue(),
                        user.userId(), user.username()));
                yield "已创建售后工单 " + ticket.ticketNo();
            }
            case "addTicketReply" -> {
                NormalizedAction normalized = normalizeTicketReply(arguments);
                JsonNode values = normalized.arguments();
                var ticket = ticketService.addMessage(values.get("ticketId").longValue(),
                        new AddTicketMessageCommand(user.userId(), user.username(),
                                values.get("content").textValue()));
                yield "已向工单 " + ticket.ticketNo() + " 追加回复";
            }
            default -> throw invalidPlan();
        };
    }

    private NormalizedAction normalize(String actionName, JsonNode arguments) {
        return switch (actionName) {
            case "createAfterSalesTicket" -> normalizeCreateTicket(arguments);
            case "addTicketReply" -> normalizeTicketReply(arguments);
            default -> throw invalidPlan();
        };
    }

    private NormalizedAction normalizeCreateTicket(JsonNode arguments) {
        requireFields(arguments, "orderId", "orderNo", "category", "priority", "subject", "description");
        Long orderId = nullablePositiveLong(arguments, "orderId");
        String orderNo = nullableText(arguments, "orderNo", 40);
        if ((orderId == null) == (orderNo == null)) throw invalidPlan();
        TicketCategory category = enumValue(arguments, "category", TicketCategory.class);
        TicketPriority priority = enumValue(arguments, "priority", TicketPriority.class);
        String subject = text(arguments, "subject", 120);
        String description = text(arguments, "description", 2000);
        ObjectNode normalized = objectMapper.createObjectNode();
        if (orderId == null) normalized.putNull("orderId"); else normalized.put("orderId", orderId);
        if (orderNo == null) normalized.putNull("orderNo"); else normalized.put("orderNo", orderNo.toUpperCase(java.util.Locale.ROOT));
        normalized.put("category", category.name());
        normalized.put("priority", priority.name());
        normalized.put("subject", subject);
        normalized.put("description", description);
        String orderReference = orderId == null
                ? "订单号：" + orderNo.toUpperCase(java.util.Locale.ROOT) : "订单 ID：" + orderId;
        String summary = "%s；分类：%s；优先级：%s；主题：%s；问题描述：%s"
                .formatted(orderReference, categoryLabel(category), priorityLabel(priority), subject, description);
        return new NormalizedAction(normalized, summary);
    }

    private NormalizedAction normalizeTicketReply(JsonNode arguments) {
        requireFields(arguments, "ticketId", "content");
        long ticketId = positiveLong(arguments, "ticketId");
        String content = text(arguments, "content", 1000);
        ObjectNode normalized = objectMapper.createObjectNode();
        normalized.put("ticketId", ticketId);
        normalized.put("content", content);
        return new NormalizedAction(normalized, "工单 ID：%d；回复内容：%s".formatted(ticketId, content));
    }

    private AgentActionConfirmation requireOwned(String token, AgentUserContext user) {
        String normalized = token == null ? "" : token.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_TOKEN_LENGTH) {
            throw new BusinessException(ErrorCode.AGENT_ACTION_NOT_FOUND);
        }
        AgentActionConfirmation confirmation = repository.findByTokenHash(hashToken(normalized))
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_ACTION_NOT_FOUND));
        // 对其他用户隐藏令牌是否真实存在，防止确认记录被探测。
        if (confirmation.userId() != user.userId()) {
            throw new BusinessException(ErrorCode.AGENT_ACTION_NOT_FOUND);
        }
        return confirmation;
    }

    private static void rejectUnavailableState(AgentActionStatus status) {
        switch (status) {
            case CANCELLED -> throw new BusinessException(ErrorCode.AGENT_ACTION_CANCELLED);
            case EXPIRED -> throw new BusinessException(ErrorCode.AGENT_ACTION_EXPIRED);
            case EXECUTING -> throw new BusinessException(ErrorCode.AGENT_ACTION_IN_PROGRESS);
            case SUCCEEDED, PENDING -> { }
        }
    }

    private AgentCapability requireDefinition(String actionName) {
        return capabilityRegistry.find(actionName)
                .filter(capability -> capability.type() == AgentCapabilityType.CONFIRM_REQUIRED)
                .orElseThrow(PendingAgentActionService::invalidPlan);
    }

    private static void requirePermission(AgentUserContext user, String permission) {
        if (!user.can(permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号缺少操作权限：" + permission);
        }
    }

    private static void requireFields(JsonNode node, String... fields) {
        if (node == null || !node.isObject()) throw invalidPlan();
        Set<String> actual = new HashSet<>();
        node.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(Set.of(fields))) throw invalidPlan();
    }

    private static long positiveLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() <= 0) {
            throw invalidPlan();
        }
        return value.longValue();
    }

    private static Long nullablePositiveLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() <= 0) throw invalidPlan();
        return value.longValue();
    }

    private static String nullableText(JsonNode node, String field, int maxLength) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) throw invalidPlan();
        String normalized = value.textValue().trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) throw invalidPlan();
        return normalized;
    }

    private static String text(JsonNode node, String field, int maxLength) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) throw invalidPlan();
        String normalized = value.textValue().trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) throw invalidPlan();
        return normalized;
    }

    private static <T extends Enum<T>> T enumValue(JsonNode node, String field, Class<T> enumType) {
        try {
            return Enum.valueOf(enumType, text(node, field, 40));
        } catch (IllegalArgumentException exception) {
            throw invalidPlan();
        }
    }

    private JsonNode parseArguments(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            throw invalidPlan();
        }
    }

    private String toJson(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Agent 操作参数序列化失败");
        }
    }

    private void safeAudit(AgentUserContext user, String actionName, String argumentsJson,
                           String status, long latencyMs, String errorCode) {
        try {
            auditService.record(user, actionName, argumentsJson, status, latencyMs, errorCode);
        } catch (RuntimeException exception) {
            log.error("Agent 操作审计记录保存失败：操作={}，用户ID={}",
                    actionName, user.userId(), exception);
        }
    }

    private static AgentActionExecution completedResult(AgentActionConfirmation confirmation,
                                                        AgentCapability definition, long latencyMs) {
        String summary = confirmation.resultSummary() == null ? "该操作已执行完成" : confirmation.resultSummary();
        return new AgentActionExecution(confirmation.actionName(), definition.displayName(), summary,
                AgentActionStatus.SUCCEEDED, latencyMs);
    }

    private static AgentActionExecution cancelledResult(AgentActionConfirmation confirmation,
                                                        AgentCapability definition) {
        return new AgentActionExecution(confirmation.actionName(), definition.displayName(), "已取消，未执行任何写操作",
                AgentActionStatus.CANCELLED, 0);
    }

    private static String createToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String categoryLabel(TicketCategory category) {
        return switch (category) {
            case RETURN_EXCHANGE -> "退换货";
            case LOGISTICS -> "物流问题";
            case PRODUCT_QUALITY -> "商品质量";
            case OTHER -> "其他";
        };
    }

    private static String priorityLabel(TicketPriority priority) {
        return switch (priority) {
            case LOW -> "低";
            case NORMAL -> "普通";
            case HIGH -> "高";
            case URGENT -> "紧急";
        };
    }

    private static BusinessException invalidPlan() {
        return new BusinessException(ErrorCode.AGENT_PLAN_INVALID,
                "模型未能生成有效的受控操作计划，请补充必要信息后重试");
    }

    private static long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private record NormalizedAction(JsonNode arguments, String summary) { }
}

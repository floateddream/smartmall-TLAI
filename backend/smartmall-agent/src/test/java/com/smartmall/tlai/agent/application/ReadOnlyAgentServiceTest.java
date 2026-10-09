package com.smartmall.tlai.agent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmall.tlai.agent.model.AgentUserContext;
import com.smartmall.tlai.agent.model.AgentPendingAction;
import com.smartmall.tlai.agent.model.AgentResponseType;
import com.smartmall.tlai.ai.application.AiChatService;
import com.smartmall.tlai.ai.model.AiChatMessage;
import com.smartmall.tlai.ai.model.AiChatResult;
import com.smartmall.tlai.ai.model.AiMessageRole;
import com.smartmall.tlai.analytics.application.AnalyticsQueryService;
import com.smartmall.tlai.business.order.application.OrderService;
import com.smartmall.tlai.business.refund.application.RefundService;
import com.smartmall.tlai.business.ticket.application.TicketService;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import com.smartmall.tlai.knowledge.application.KnowledgeSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadOnlyAgentServiceTest {
    @Mock AiChatService aiChatService;
    @Mock KnowledgeSearchService knowledgeSearchService;
    @Mock OrderService orderService;
    @Mock RefundService refundService;
    @Mock TicketService ticketService;
    @Mock AnalyticsQueryService analyticsQueryService;
    @Mock AgentToolAuditService auditService;
    @Mock PendingAgentActionService pendingActionService;

    @Test
    void executesPermittedOrderQueryAndReturnsTrace() {
        when(aiChatService.chatWithSystemPrompt(anyList(), anyString()))
                .thenReturn(new AiChatResult("""
                        {"action":"TOOL_CALL","intent":"queryOrder","arguments":{"id":null,"keyword":"SM2026","status":""},"missingFields":[]}
                        """, "planner", 20))
                .thenReturn(new AiChatResult("找到相关订单。", "deepseek-chat", 40));
        when(orderService.list(1, 5, "SM2026", ""))
                .thenReturn(new PageResponse<>(List.of(), 0, 1, 5));

        var result = service().chat(messages(), new AgentUserContext(7L, "admin", Set.of("order:view")));

        assertThat(result.content()).isEqualTo("找到相关订单。");
        assertThat(result.tools()).singleElement().satisfies(tool -> {
            assertThat(tool.toolName()).isEqualTo("queryOrder");
            assertThat(tool.displayName()).isEqualTo("订单查询");
        });
        verify(orderService).list(1, 5, "SM2026", "");
        verify(auditService).record(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("queryOrder"), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("SUCCESS"), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void rejectsToolWhenCurrentUserLacksPermission() {
        when(aiChatService.chatWithSystemPrompt(anyList(), anyString()))
                .thenReturn(new AiChatResult("""
                        {"action":"TOOL_CALL","intent":"queryRefund","arguments":{"id":null,"keyword":"RF","status":""},"missingFields":[]}
                        """, "planner", 20));

        assertThatThrownBy(() -> service().chat(messages(),
                new AgentUserContext(9L, "operator", Set.of("ai:chat"))))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        verify(refundService, never()).list(1, 5, "RF", "");
        verify(auditService).record(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("queryRefund"), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("FAILED"), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.eq("FORBIDDEN"));
    }

    @Test
    void convertsUnknownToolFromModelToSafeClarification() {
        when(aiChatService.chatWithSystemPrompt(anyList(), anyString()))
                .thenReturn(new AiChatResult("""
                        {"action":"TOOL_CALL","intent":"deleteOrder","arguments":{},"missingFields":[]}
                        """, "planner", 20));

        var result = service().chat(messages(),
                new AgentUserContext(7L, "admin", Set.of("order:view")));

        assertThat(result.responseType()).isEqualTo(AgentResponseType.CLARIFICATION);
        assertThat(result.content()).contains("没有修改任何数据");
        verify(orderService, never()).get(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void createsConfirmationPlanWithoutExecutingTicketWrite() {
        when(aiChatService.chatWithSystemPrompt(anyList(), anyString()))
                .thenReturn(new AiChatResult("""
                        {"action":"ACTION_PLAN","intent":"createAfterSalesTicket","arguments":{"orderId":12,"orderNo":null,"category":"LOGISTICS","priority":"NORMAL","subject":"物流停滞","description":"三天没有更新"},"missingFields":[]}
                        """, "planner", 20));
        var pending = new AgentPendingAction("token", "createAfterSalesTicket", "创建售后工单",
                "订单 ID：12", OffsetDateTime.now().plusMinutes(10));
        when(pendingActionService.create(org.mockito.ArgumentMatchers.eq("createAfterSalesTicket"),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(pending);

        var result = service().chat(messages(),
                new AgentUserContext(7L, "admin", Set.of("ticket:create")));

        assertThat(result.pendingAction()).isEqualTo(pending);
        verify(ticketService, never()).create(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void convertsInvalidWritePlanToGuidanceInsteadOfTechnicalError() {
        when(aiChatService.chatWithSystemPrompt(anyList(), anyString()))
                .thenReturn(new AiChatResult("""
                        {"action":"ACTION_PLAN","intent":"createAfterSalesTicket","arguments":{"orderId":null,"orderNo":"","category":"LOGISTICS","priority":"NORMAL","subject":"","description":""},"missingFields":[]}
                        """, "planner", 20));
        when(pendingActionService.create(org.mockito.ArgumentMatchers.eq("createAfterSalesTicket"),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.AGENT_PLAN_INVALID));

        var result = service().chat(messages(),
                new AgentUserContext(7L, "admin", Set.of("ticket:create")));

        assertThat(result.responseType()).isEqualTo(AgentResponseType.CLARIFICATION);
        assertThat(result.content()).contains("订单号", "工单分类", "问题主题", "具体问题描述");
        assertThat(result.pendingAction()).isNull();
    }

    @Test
    void returnsDeterministicGuidanceWhenWriteParametersAreMissing() {
        when(aiChatService.chatWithSystemPrompt(anyList(), anyString()))
                .thenReturn(new AiChatResult("""
                        {"action":"CLARIFY","intent":"createAfterSalesTicket","arguments":{},"missingFields":["orderNo","subject","description"]}
                        """, "planner", 20));

        var result = service().chat(messages(),
                new AgentUserContext(7L, "admin", Set.of("ticket:create")));

        assertThat(result.responseType()).isEqualTo(AgentResponseType.CLARIFICATION);
        assertThat(result.missingFields()).containsExactly("orderNo", "subject", "description");
        assertThat(result.content()).contains("订单号", "问题主题", "具体问题描述");
        assertThat(result.pendingAction()).isNull();
        verifyNoInteractions(pendingActionService);
    }

    @Test
    void asksBeforeReusingPreviousWriteParameters() {
        var result = service().chat(List.of(new AiChatMessage(AiMessageRole.USER, "再创建一个")),
                new AgentUserContext(7L, "admin", Set.of("ticket:create")));

        assertThat(result.responseType()).isEqualTo(AgentResponseType.CLARIFICATION);
        assertThat(result.content()).contains("不会自动沿用", "上一笔订单");
        assertThat(result.pendingAction()).isNull();
        verifyNoInteractions(aiChatService, pendingActionService);
    }

    @Test
    void explainsUnsupportedOperationWithoutThrowingBusinessError() {
        when(aiChatService.chatWithSystemPrompt(anyList(), anyString()))
                .thenReturn(new AiChatResult("""
                        {"action":"UNSUPPORTED","intent":"approveRefund","arguments":{},"missingFields":[]}
                        """, "planner", 20));

        var result = service().chat(messages(),
                new AgentUserContext(7L, "admin", Set.of("refund:view")));

        assertThat(result.responseType()).isEqualTo(AgentResponseType.UNSUPPORTED);
        assertThat(result.content()).contains("审核退款", "业务页面");
        assertThat(result.pendingAction()).isNull();
    }

    private ReadOnlyAgentService service() {
        return new ReadOnlyAgentService(aiChatService, knowledgeSearchService, orderService,
                refundService, ticketService, analyticsQueryService, auditService, pendingActionService,
                new AgentCapabilityRegistry(), new ObjectMapper().findAndRegisterModules());
    }

    private List<AiChatMessage> messages() {
        return List.of(new AiChatMessage(AiMessageRole.USER, "帮我查一下"));
    }
}

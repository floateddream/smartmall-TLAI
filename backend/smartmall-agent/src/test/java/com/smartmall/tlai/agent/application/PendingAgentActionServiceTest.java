package com.smartmall.tlai.agent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmall.tlai.agent.model.AgentActionConfirmation;
import com.smartmall.tlai.agent.model.AgentActionStatus;
import com.smartmall.tlai.agent.model.AgentUserContext;
import com.smartmall.tlai.agent.repository.AgentActionConfirmationRepository;
import com.smartmall.tlai.business.order.application.OrderService;
import com.smartmall.tlai.business.order.domain.Order;
import com.smartmall.tlai.business.ticket.application.TicketService;
import com.smartmall.tlai.business.ticket.domain.AfterSalesTicket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingAgentActionServiceTest {
    @Mock AgentActionConfirmationRepository repository;
    @Mock OrderService orderService;
    @Mock TicketService ticketService;
    @Mock AgentToolAuditService auditService;

    @Test
    void creatingPlanPersistsOnlyConfirmationAndDoesNotWriteTicket() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        var arguments = mapper.readTree("""
                {"orderId":null,"orderNo":"SM20260924082608B125E3","category":"LOGISTICS","priority":"NORMAL","subject":"物流停滞","description":"三天没有更新"}
                """);

        var result = service(mapper).create("createAfterSalesTicket", arguments,
                new AgentUserContext(7L, "admin", Set.of("ticket:create")));

        assertThat(result.token()).isNotBlank();
        assertThat(result.summary()).contains("订单号：SM20260924082608B125E3", "物流问题");
        verify(repository).insert(any());
        verify(ticketService, never()).create(any());
    }

    @Test
    void confirmationExecutesTicketWriteOnce() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        var confirmation = new AgentActionConfirmation(9L, "hash", 7L, "admin",
                "createAfterSalesTicket",
                "{\"orderId\":null,\"orderNo\":\"SM20260924082608B125E3\",\"category\":\"LOGISTICS\",\"priority\":\"NORMAL\",\"subject\":\"物流停滞\",\"description\":\"三天没有更新\"}",
                AgentActionStatus.PENDING, now.plusMinutes(5), null, now, null);
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(confirmation));
        when(repository.markExecuting(anyLong(), anyLong(), any())).thenReturn(true);
        AfterSalesTicket ticket = mock(AfterSalesTicket.class);
        Order order = mock(Order.class);
        when(order.id()).thenReturn(12L);
        when(orderService.getByOrderNo("SM20260924082608B125E3")).thenReturn(order);
        when(ticket.ticketNo()).thenReturn("TK202610080001");
        when(ticketService.create(any())).thenReturn(ticket);

        var result = service(new ObjectMapper().findAndRegisterModules()).confirm("raw-token",
                new AgentUserContext(7L, "admin", Set.of("ticket:create")));

        assertThat(result.status()).isEqualTo(AgentActionStatus.SUCCEEDED);
        assertThat(result.summary()).contains("TK202610080001");
        verify(ticketService).create(any());
        verify(orderService).getByOrderNo("SM20260924082608B125E3");
        verify(repository).markSucceeded(9L, "已创建售后工单 TK202610080001");
    }

    @Test
    void repeatedConfirmationReturnsStoredResultWithoutWritingAgain() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        var confirmation = new AgentActionConfirmation(9L, "hash", 7L, "admin",
                "addTicketReply", "{\"ticketId\":3,\"content\":\"已收到\"}",
                AgentActionStatus.SUCCEEDED, now.plusMinutes(5), "已向工单 TK001 追加回复", now, now);
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(confirmation));

        var result = service(new ObjectMapper().findAndRegisterModules()).confirm("raw-token",
                new AgentUserContext(7L, "admin", Set.of("ticket:reply")));

        assertThat(result.summary()).isEqualTo("已向工单 TK001 追加回复");
        verify(ticketService, never()).addMessage(anyLong(), any());
    }

    @Test
    void cancellationChangesStateWithoutWritingTicket() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        var confirmation = new AgentActionConfirmation(9L, "hash", 7L, "admin",
                "addTicketReply", "{\"ticketId\":3,\"content\":\"已收到\"}",
                AgentActionStatus.PENDING, now.plusMinutes(5), null, now, null);
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(confirmation));
        when(repository.cancel(9L, 7L)).thenReturn(true);

        var result = service(new ObjectMapper().findAndRegisterModules()).cancel("raw-token",
                new AgentUserContext(7L, "admin", Set.of("ticket:reply")));

        assertThat(result.status()).isEqualTo(AgentActionStatus.CANCELLED);
        verify(ticketService, never()).addMessage(anyLong(), any());
        verify(ticketService, never()).create(any());
    }

    private PendingAgentActionService service(ObjectMapper mapper) {
        return new PendingAgentActionService(repository, orderService, ticketService, auditService,
                new AgentCapabilityRegistry(), mapper);
    }
}

package com.smartmall.tlai.business.ticket.application;

import com.smartmall.tlai.business.order.repository.OrderRepository;
import com.smartmall.tlai.business.ticket.domain.AfterSalesTicket;
import com.smartmall.tlai.business.ticket.domain.TicketCategory;
import com.smartmall.tlai.business.ticket.domain.TicketPriority;
import com.smartmall.tlai.business.ticket.domain.TicketStatus;
import com.smartmall.tlai.business.ticket.repository.TicketMessageRepository;
import com.smartmall.tlai.business.ticket.repository.TicketRepository;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    @Mock TicketRepository ticketRepository;
    @Mock TicketMessageRepository messageRepository;
    @Mock OrderRepository orderRepository;

    @Test
    void statusCanOnlyMoveToNextStep() {
        when(ticketRepository.findById(4)).thenReturn(Optional.of(ticket(TicketStatus.OPEN)));

        assertThatThrownBy(() -> service().updateStatus(4,
                new UpdateTicketStatusCommand(TicketStatus.RESOLVED, 2, "admin", null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> org.assertj.core.api.Assertions.assertThat(error.getErrorCode())
                                .isEqualTo(ErrorCode.TICKET_STATUS_INVALID));
        verify(ticketRepository, never()).transitionStatus(anyLong(), any(), any(), any());
    }

    @Test
    void statusTransitionWritesTimelineMessage() {
        when(ticketRepository.findById(4)).thenReturn(
                Optional.of(ticket(TicketStatus.OPEN)), Optional.of(ticket(TicketStatus.PROCESSING)));
        when(ticketRepository.transitionStatus(anyLong(), any(), any(), any())).thenReturn(true);

        service().updateStatus(4,
                new UpdateTicketStatusCommand(TicketStatus.PROCESSING, 2, "admin", "开始处理"));

        verify(messageRepository).insert(any());
    }

    @Test
    void closedTicketRejectsNewReply() {
        when(ticketRepository.findById(4)).thenReturn(Optional.of(ticket(TicketStatus.CLOSED)));

        assertThatThrownBy(() -> service().addMessage(4,
                new AddTicketMessageCommand(2, "admin", "继续回复")))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> org.assertj.core.api.Assertions.assertThat(error.getErrorCode())
                                .isEqualTo(ErrorCode.TICKET_CLOSED));
        verify(messageRepository, never()).insert(any());
    }

    private TicketService service() {
        return new TicketService(ticketRepository, messageRepository, orderRepository);
    }

    private AfterSalesTicket ticket(TicketStatus status) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new AfterSalesTicket(4L, "TK202609240001", 8L, "SM202609240001", 7L, "张三",
                TicketCategory.LOGISTICS, TicketPriority.NORMAL, status, "物流一直未更新", "三天没有物流信息",
                1L, "operator", null, null, now, now, List.of());
    }
}

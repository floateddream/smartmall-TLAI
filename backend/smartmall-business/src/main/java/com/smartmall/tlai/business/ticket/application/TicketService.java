package com.smartmall.tlai.business.ticket.application;

import com.smartmall.tlai.business.order.domain.Order;
import com.smartmall.tlai.business.order.repository.OrderRepository;
import com.smartmall.tlai.business.ticket.domain.AfterSalesTicket;
import com.smartmall.tlai.business.ticket.domain.TicketCategory;
import com.smartmall.tlai.business.ticket.domain.TicketMessage;
import com.smartmall.tlai.business.ticket.domain.TicketMessageType;
import com.smartmall.tlai.business.ticket.domain.TicketStatus;
import com.smartmall.tlai.business.ticket.repository.TicketMessageRepository;
import com.smartmall.tlai.business.ticket.repository.TicketRepository;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class TicketService {
    private static final DateTimeFormatter NUMBER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final OrderRepository orderRepository;

    public TicketService(TicketRepository ticketRepository, TicketMessageRepository messageRepository,
                         OrderRepository orderRepository) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AfterSalesTicket> list(int page, int size, String keyword,
                                               String statusValue, String categoryValue) {
        String search = keyword == null ? "" : keyword.trim();
        TicketStatus status = parseStatus(statusValue);
        TicketCategory category = parseCategory(categoryValue);
        return new PageResponse<>(ticketRepository.findPage(page, size, search, status, category),
                ticketRepository.count(search, status, category), page, size);
    }

    @Transactional(readOnly = true)
    public AfterSalesTicket get(long id) {
        return requireTicket(id);
    }

    @Transactional
    public AfterSalesTicket create(CreateTicketCommand command) {
        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        AfterSalesTicket ticket = new AfterSalesTicket(null, createTicketNo(now), order.id(), order.orderNo(),
                order.customerId(), order.customerName(), command.category(), command.priority(), TicketStatus.OPEN,
                command.subject().trim(), command.description().trim(), command.creatorId(), command.creatorName(),
                null, null, now, now, null);
        return ticketRepository.insert(ticket);
    }

    @Transactional
    public AfterSalesTicket addMessage(long id, AddTicketMessageCommand command) {
        AfterSalesTicket ticket = requireTicket(id);
        if (ticket.status() == TicketStatus.CLOSED) {
            throw new BusinessException(ErrorCode.TICKET_CLOSED);
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        messageRepository.insert(new TicketMessage(null, id, TicketMessageType.REPLY,
                command.senderId(), command.senderName(), command.content().trim(), null, null, now));
        ticketRepository.touch(id, now);
        return requireTicket(id);
    }

    @Transactional
    public AfterSalesTicket updateStatus(long id, UpdateTicketStatusCommand command) {
        AfterSalesTicket ticket = requireTicket(id);
        if (!ticket.status().canTransitionTo(command.status())) {
            throw new BusinessException(ErrorCode.TICKET_STATUS_INVALID);
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (!ticketRepository.transitionStatus(id, ticket.status(), command.status(), now)) {
            throw new BusinessException(ErrorCode.TICKET_STATUS_INVALID);
        }
        messageRepository.insert(new TicketMessage(null, id, TicketMessageType.STATUS_CHANGE,
                command.operatorId(), command.operatorName(), normalizeOptional(command.remark()),
                ticket.status(), command.status(), now));
        return requireTicket(id);
    }

    private AfterSalesTicket requireTicket(long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND));
    }

    private TicketStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return TicketStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工单状态筛选值不正确");
        }
    }

    private TicketCategory parseCategory(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return TicketCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工单分类筛选值不正确");
        }
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String createTicketNo(OffsetDateTime now) {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        return "TK" + NUMBER_TIME_FORMAT.format(now) + random;
    }
}

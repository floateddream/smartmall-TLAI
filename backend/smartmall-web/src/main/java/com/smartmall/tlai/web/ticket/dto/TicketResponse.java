package com.smartmall.tlai.web.ticket.dto;

import com.smartmall.tlai.business.ticket.domain.AfterSalesTicket;
import com.smartmall.tlai.business.ticket.domain.TicketCategory;
import com.smartmall.tlai.business.ticket.domain.TicketPriority;
import com.smartmall.tlai.business.ticket.domain.TicketStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record TicketResponse(Long id, String ticketNo, Long orderId, String orderNo,
                             Long customerId, String customerName,
                             TicketCategory category, TicketPriority priority, TicketStatus status,
                             String subject, String description,
                             Long creatorId, String creatorName,
                             OffsetDateTime resolvedAt, OffsetDateTime closedAt,
                             OffsetDateTime createdAt, OffsetDateTime updatedAt,
                             List<TicketMessageResponse> messages) {
    public static TicketResponse from(AfterSalesTicket ticket) {
        return new TicketResponse(ticket.id(), ticket.ticketNo(), ticket.orderId(), ticket.orderNo(),
                ticket.customerId(), ticket.customerName(), ticket.category(), ticket.priority(), ticket.status(),
                ticket.subject(), ticket.description(), ticket.creatorId(), ticket.creatorName(),
                ticket.resolvedAt(), ticket.closedAt(), ticket.createdAt(), ticket.updatedAt(),
                ticket.messages().stream().map(TicketMessageResponse::from).toList());
    }
}

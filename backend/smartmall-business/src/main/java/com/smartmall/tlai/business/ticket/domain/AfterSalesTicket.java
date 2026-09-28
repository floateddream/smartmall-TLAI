package com.smartmall.tlai.business.ticket.domain;

import java.time.OffsetDateTime;
import java.util.List;

public record AfterSalesTicket(Long id, String ticketNo, Long orderId, String orderNo,
                               Long customerId, String customerName,
                               TicketCategory category, TicketPriority priority, TicketStatus status,
                               String subject, String description,
                               Long creatorId, String creatorName,
                               OffsetDateTime resolvedAt, OffsetDateTime closedAt,
                               OffsetDateTime createdAt, OffsetDateTime updatedAt,
                               List<TicketMessage> messages) {
    public AfterSalesTicket {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }
}

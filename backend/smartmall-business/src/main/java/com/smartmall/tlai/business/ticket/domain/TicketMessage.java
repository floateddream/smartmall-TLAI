package com.smartmall.tlai.business.ticket.domain;

import java.time.OffsetDateTime;

public record TicketMessage(Long id, Long ticketId, TicketMessageType type,
                            Long senderId, String senderName, String content,
                            TicketStatus fromStatus, TicketStatus toStatus,
                            OffsetDateTime createdAt) {
}

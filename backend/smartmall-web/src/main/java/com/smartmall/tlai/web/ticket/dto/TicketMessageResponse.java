package com.smartmall.tlai.web.ticket.dto;

import com.smartmall.tlai.business.ticket.domain.TicketMessage;
import com.smartmall.tlai.business.ticket.domain.TicketMessageType;
import com.smartmall.tlai.business.ticket.domain.TicketStatus;

import java.time.OffsetDateTime;

public record TicketMessageResponse(Long id, TicketMessageType type,
                                    Long senderId, String senderName, String content,
                                    TicketStatus fromStatus, TicketStatus toStatus,
                                    OffsetDateTime createdAt) {
    public static TicketMessageResponse from(TicketMessage message) {
        return new TicketMessageResponse(message.id(), message.type(), message.senderId(), message.senderName(),
                message.content(), message.fromStatus(), message.toStatus(), message.createdAt());
    }
}

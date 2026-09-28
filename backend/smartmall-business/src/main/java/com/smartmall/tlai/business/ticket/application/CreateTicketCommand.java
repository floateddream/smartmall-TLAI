package com.smartmall.tlai.business.ticket.application;

import com.smartmall.tlai.business.ticket.domain.TicketCategory;
import com.smartmall.tlai.business.ticket.domain.TicketPriority;

public record CreateTicketCommand(long orderId, TicketCategory category, TicketPriority priority,
                                  String subject, String description,
                                  long creatorId, String creatorName) {
}

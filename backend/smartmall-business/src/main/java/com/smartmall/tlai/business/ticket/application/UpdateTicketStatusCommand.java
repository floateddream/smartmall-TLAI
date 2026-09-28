package com.smartmall.tlai.business.ticket.application;

import com.smartmall.tlai.business.ticket.domain.TicketStatus;

public record UpdateTicketStatusCommand(TicketStatus status, long operatorId,
                                        String operatorName, String remark) {
}

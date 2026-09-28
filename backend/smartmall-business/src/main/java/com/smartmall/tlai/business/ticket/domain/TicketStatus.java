package com.smartmall.tlai.business.ticket.domain;

public enum TicketStatus {
    OPEN,
    PROCESSING,
    RESOLVED,
    CLOSED;

    public boolean canTransitionTo(TicketStatus target) {
        return switch (this) {
            case OPEN -> target == PROCESSING;
            case PROCESSING -> target == RESOLVED;
            case RESOLVED -> target == CLOSED;
            case CLOSED -> false;
        };
    }
}

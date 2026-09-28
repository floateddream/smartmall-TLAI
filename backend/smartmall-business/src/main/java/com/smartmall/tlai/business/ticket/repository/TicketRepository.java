package com.smartmall.tlai.business.ticket.repository;

import com.smartmall.tlai.business.ticket.domain.AfterSalesTicket;
import com.smartmall.tlai.business.ticket.domain.TicketCategory;
import com.smartmall.tlai.business.ticket.domain.TicketStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository {
    long count(String keyword, TicketStatus status, TicketCategory category);
    List<AfterSalesTicket> findPage(int page, int size, String keyword,
                                    TicketStatus status, TicketCategory category);
    Optional<AfterSalesTicket> findById(long id);
    AfterSalesTicket insert(AfterSalesTicket ticket);
    boolean transitionStatus(long id, TicketStatus expected, TicketStatus target, OffsetDateTime changedAt);
    void touch(long id, OffsetDateTime changedAt);
}

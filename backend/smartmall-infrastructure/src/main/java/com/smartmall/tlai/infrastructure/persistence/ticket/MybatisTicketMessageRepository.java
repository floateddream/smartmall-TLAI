package com.smartmall.tlai.infrastructure.persistence.ticket;

import com.smartmall.tlai.business.ticket.domain.TicketMessage;
import com.smartmall.tlai.business.ticket.repository.TicketMessageRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisTicketMessageRepository implements TicketMessageRepository {
    private final TicketMessageMapper mapper;

    public MybatisTicketMessageRepository(TicketMessageMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TicketMessage insert(TicketMessage message) {
        TicketMessagePersistenceEntity entity = new TicketMessagePersistenceEntity();
        entity.setId(message.id());
        entity.setTicketId(message.ticketId());
        entity.setMessageType(message.type().name());
        entity.setSenderId(message.senderId());
        entity.setSenderName(message.senderName());
        entity.setContent(message.content());
        entity.setFromStatus(message.fromStatus() == null ? null : message.fromStatus().name());
        entity.setToStatus(message.toStatus() == null ? null : message.toStatus().name());
        entity.setCreatedAt(message.createdAt());
        mapper.insert(entity);
        return new TicketMessage(entity.getId(), entity.getTicketId(), message.type(), entity.getSenderId(),
                entity.getSenderName(), entity.getContent(), message.fromStatus(), message.toStatus(),
                entity.getCreatedAt());
    }
}

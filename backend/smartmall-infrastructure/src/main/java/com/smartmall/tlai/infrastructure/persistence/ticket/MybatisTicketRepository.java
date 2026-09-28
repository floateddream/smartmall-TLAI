package com.smartmall.tlai.infrastructure.persistence.ticket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartmall.tlai.business.ticket.domain.AfterSalesTicket;
import com.smartmall.tlai.business.ticket.domain.TicketCategory;
import com.smartmall.tlai.business.ticket.domain.TicketMessage;
import com.smartmall.tlai.business.ticket.domain.TicketMessageType;
import com.smartmall.tlai.business.ticket.domain.TicketPriority;
import com.smartmall.tlai.business.ticket.domain.TicketStatus;
import com.smartmall.tlai.business.ticket.repository.TicketRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class MybatisTicketRepository implements TicketRepository {
    private final TicketMapper ticketMapper;
    private final TicketMessageMapper messageMapper;

    public MybatisTicketRepository(TicketMapper ticketMapper, TicketMessageMapper messageMapper) {
        this.ticketMapper = ticketMapper;
        this.messageMapper = messageMapper;
    }

    @Override
    public long count(String keyword, TicketStatus status, TicketCategory category) {
        return ticketMapper.selectCount(filter(keyword, status, category));
    }

    @Override
    public List<AfterSalesTicket> findPage(int page, int size, String keyword,
                                           TicketStatus status, TicketCategory category) {
        long offset = ((long) page - 1) * size;
        return ticketMapper.selectList(filter(keyword, status, category)
                        .orderByDesc(TicketPersistenceEntity::getId)
                        .last("LIMIT " + size + " OFFSET " + offset))
                .stream().map(entity -> toDomain(entity, List.of())).toList();
    }

    @Override
    public Optional<AfterSalesTicket> findById(long id) {
        TicketPersistenceEntity entity = ticketMapper.selectById(id);
        if (entity == null) return Optional.empty();
        List<TicketMessage> messages = messageMapper.selectList(
                        Wrappers.<TicketMessagePersistenceEntity>lambdaQuery()
                                .eq(TicketMessagePersistenceEntity::getTicketId, id)
                                .orderByAsc(TicketMessagePersistenceEntity::getId))
                .stream().map(this::toDomainMessage).toList();
        return Optional.of(toDomain(entity, messages));
    }

    @Override
    public AfterSalesTicket insert(AfterSalesTicket ticket) {
        TicketPersistenceEntity entity = toEntity(ticket);
        ticketMapper.insert(entity);
        return toDomain(entity, List.of());
    }

    @Override
    public boolean transitionStatus(long id, TicketStatus expected, TicketStatus target, OffsetDateTime changedAt) {
        return ticketMapper.transitionStatus(id, expected.name(), target.name(), changedAt) == 1;
    }

    @Override
    public void touch(long id, OffsetDateTime changedAt) {
        ticketMapper.touch(id, changedAt);
    }

    private LambdaQueryWrapper<TicketPersistenceEntity> filter(String keyword, TicketStatus status,
                                                                 TicketCategory category) {
        LambdaQueryWrapper<TicketPersistenceEntity> query = Wrappers.lambdaQuery(TicketPersistenceEntity.class);
        if (!keyword.isEmpty()) {
            query.and(part -> part.like(TicketPersistenceEntity::getTicketNo, keyword.toUpperCase(Locale.ROOT))
                    .or().like(TicketPersistenceEntity::getOrderNo, keyword.toUpperCase(Locale.ROOT))
                    .or().like(TicketPersistenceEntity::getCustomerName, keyword)
                    .or().like(TicketPersistenceEntity::getSubject, keyword));
        }
        if (status != null) query.eq(TicketPersistenceEntity::getStatus, status.name());
        if (category != null) query.eq(TicketPersistenceEntity::getCategory, category.name());
        return query;
    }

    private AfterSalesTicket toDomain(TicketPersistenceEntity entity, List<TicketMessage> messages) {
        return new AfterSalesTicket(entity.getId(), entity.getTicketNo(), entity.getOrderId(), entity.getOrderNo(),
                entity.getCustomerId(), entity.getCustomerName(), TicketCategory.valueOf(entity.getCategory()),
                TicketPriority.valueOf(entity.getPriority()), TicketStatus.valueOf(entity.getStatus()),
                entity.getSubject(), entity.getDescription(), entity.getCreatorId(), entity.getCreatorName(),
                entity.getResolvedAt(), entity.getClosedAt(), entity.getCreatedAt(), entity.getUpdatedAt(), messages);
    }

    private TicketMessage toDomainMessage(TicketMessagePersistenceEntity entity) {
        return new TicketMessage(entity.getId(), entity.getTicketId(),
                TicketMessageType.valueOf(entity.getMessageType()), entity.getSenderId(), entity.getSenderName(),
                entity.getContent(), parseStatus(entity.getFromStatus()), parseStatus(entity.getToStatus()),
                entity.getCreatedAt());
    }

    private TicketStatus parseStatus(String status) {
        return status == null ? null : TicketStatus.valueOf(status);
    }

    private TicketPersistenceEntity toEntity(AfterSalesTicket ticket) {
        TicketPersistenceEntity entity = new TicketPersistenceEntity();
        entity.setId(ticket.id());
        entity.setTicketNo(ticket.ticketNo());
        entity.setOrderId(ticket.orderId());
        entity.setOrderNo(ticket.orderNo());
        entity.setCustomerId(ticket.customerId());
        entity.setCustomerName(ticket.customerName());
        entity.setCategory(ticket.category().name());
        entity.setPriority(ticket.priority().name());
        entity.setStatus(ticket.status().name());
        entity.setSubject(ticket.subject());
        entity.setDescription(ticket.description());
        entity.setCreatorId(ticket.creatorId());
        entity.setCreatorName(ticket.creatorName());
        entity.setResolvedAt(ticket.resolvedAt());
        entity.setClosedAt(ticket.closedAt());
        entity.setCreatedAt(ticket.createdAt());
        entity.setUpdatedAt(ticket.updatedAt());
        return entity;
    }
}

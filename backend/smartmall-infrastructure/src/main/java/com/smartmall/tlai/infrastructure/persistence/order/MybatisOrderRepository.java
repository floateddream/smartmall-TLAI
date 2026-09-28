package com.smartmall.tlai.infrastructure.persistence.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartmall.tlai.business.order.domain.Order;
import com.smartmall.tlai.business.order.domain.OrderItem;
import com.smartmall.tlai.business.order.domain.OrderStatus;
import com.smartmall.tlai.business.order.repository.OrderRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class MybatisOrderRepository implements OrderRepository {
    private final OrderMapper orderMapper;
    private final OrderItemMapper itemMapper;

    public MybatisOrderRepository(OrderMapper orderMapper, OrderItemMapper itemMapper) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
    }

    @Override
    public long count(String keyword, OrderStatus status) {
        return orderMapper.selectCount(filter(keyword, status));
    }

    @Override
    public List<Order> findPage(int page, int size, String keyword, OrderStatus status) {
        long offset = ((long) page - 1) * size;
        LambdaQueryWrapper<OrderPersistenceEntity> query = filter(keyword, status)
                .orderByDesc(OrderPersistenceEntity::getId)
                .last("LIMIT " + size + " OFFSET " + offset);
        // 列表暂不加载明细，避免每行额外执行一条 SQL；详情接口负责返回完整商品项。
        return orderMapper.selectList(query).stream().map(entity -> toDomain(entity, List.of())).toList();
    }

    @Override
    public Optional<Order> findById(long id) {
        OrderPersistenceEntity entity = orderMapper.selectById(id);
        if (entity == null) return Optional.empty();
        List<OrderItem> items = itemMapper.selectList(Wrappers.<OrderItemPersistenceEntity>lambdaQuery()
                        .eq(OrderItemPersistenceEntity::getOrderId, id)
                        .orderByAsc(OrderItemPersistenceEntity::getId))
                .stream().map(this::toDomainItem).toList();
        return Optional.of(toDomain(entity, items));
    }

    @Override
    public Order insert(Order order) {
        OrderPersistenceEntity entity = toEntity(order);
        orderMapper.insert(entity);
        List<OrderItem> savedItems = new ArrayList<>(order.items().size());
        for (OrderItem item : order.items()) {
            OrderItemPersistenceEntity itemEntity = toEntity(entity.getId(), item);
            itemMapper.insert(itemEntity);
            savedItems.add(toDomainItem(itemEntity));
        }
        return toDomain(entity, savedItems);
    }

    @Override
    public boolean markPaid(long id, OffsetDateTime changedAt) {
        return orderMapper.markPaid(id, changedAt) == 1;
    }

    @Override
    public boolean markCancelled(long id, OffsetDateTime changedAt) {
        return orderMapper.markCancelled(id, changedAt) == 1;
    }

    @Override
    public boolean markRefunding(long id, OffsetDateTime changedAt) {
        return orderMapper.markRefunding(id, changedAt) == 1;
    }

    @Override
    public boolean markRefunded(long id, OffsetDateTime changedAt) {
        return orderMapper.markRefunded(id, changedAt) == 1;
    }

    @Override
    public boolean restorePaidFromRefunding(long id, OffsetDateTime changedAt) {
        return orderMapper.restorePaidFromRefunding(id, changedAt) == 1;
    }

    private LambdaQueryWrapper<OrderPersistenceEntity> filter(String keyword, OrderStatus status) {
        LambdaQueryWrapper<OrderPersistenceEntity> query = Wrappers.lambdaQuery(OrderPersistenceEntity.class);
        if (!keyword.isEmpty()) {
            query.and(part -> part.like(OrderPersistenceEntity::getOrderNo, keyword.toUpperCase(Locale.ROOT))
                    .or().like(OrderPersistenceEntity::getCustomerName, keyword));
        }
        if (status != null) query.eq(OrderPersistenceEntity::getStatus, status.name());
        return query;
    }

    private Order toDomain(OrderPersistenceEntity entity, List<OrderItem> items) {
        return new Order(entity.getId(), entity.getOrderNo(), entity.getCustomerId(), entity.getCustomerName(),
                entity.getTotalAmount(), OrderStatus.valueOf(entity.getStatus()), items, entity.getPaidAt(),
                entity.getCancelledAt(), entity.getRefundedAt(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private OrderItem toDomainItem(OrderItemPersistenceEntity entity) {
        return new OrderItem(entity.getId(), entity.getProductId(), entity.getProductSku(),
                entity.getProductName(), entity.getUnitPrice(), entity.getQuantity(), entity.getSubtotal());
    }

    private OrderPersistenceEntity toEntity(Order order) {
        OrderPersistenceEntity entity = new OrderPersistenceEntity();
        entity.setId(order.id());
        entity.setOrderNo(order.orderNo());
        entity.setCustomerId(order.customerId());
        entity.setCustomerName(order.customerName());
        entity.setTotalAmount(order.totalAmount());
        entity.setStatus(order.status().name());
        entity.setPaidAt(order.paidAt());
        entity.setCancelledAt(order.cancelledAt());
        entity.setRefundedAt(order.refundedAt());
        entity.setCreatedAt(order.createdAt());
        entity.setUpdatedAt(order.updatedAt());
        return entity;
    }

    private OrderItemPersistenceEntity toEntity(long orderId, OrderItem item) {
        OrderItemPersistenceEntity entity = new OrderItemPersistenceEntity();
        entity.setId(item.id());
        entity.setOrderId(orderId);
        entity.setProductId(item.productId());
        entity.setProductSku(item.productSku());
        entity.setProductName(item.productName());
        entity.setUnitPrice(item.unitPrice());
        entity.setQuantity(item.quantity());
        entity.setSubtotal(item.subtotal());
        return entity;
    }
}

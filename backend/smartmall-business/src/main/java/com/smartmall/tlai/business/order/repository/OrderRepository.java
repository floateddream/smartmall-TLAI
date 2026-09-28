package com.smartmall.tlai.business.order.repository;

import com.smartmall.tlai.business.order.domain.Order;
import com.smartmall.tlai.business.order.domain.OrderStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** 订单持久化端口，业务层通过返回值判断并发状态更新是否成功。 */
public interface OrderRepository {
    long count(String keyword, OrderStatus status);
    List<Order> findPage(int page, int size, String keyword, OrderStatus status);
    Optional<Order> findById(long id);
    Order insert(Order order);
    boolean markPaid(long id, OffsetDateTime changedAt);
    boolean markCancelled(long id, OffsetDateTime changedAt);
    boolean markRefunding(long id, OffsetDateTime changedAt);
    boolean markRefunded(long id, OffsetDateTime changedAt);
    boolean restorePaidFromRefunding(long id, OffsetDateTime changedAt);
}

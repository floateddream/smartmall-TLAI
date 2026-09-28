package com.smartmall.tlai.business.order.application;

import com.smartmall.tlai.business.customer.domain.Customer;
import com.smartmall.tlai.business.customer.repository.CustomerRepository;
import com.smartmall.tlai.business.order.domain.Order;
import com.smartmall.tlai.business.order.domain.OrderItem;
import com.smartmall.tlai.business.order.domain.OrderStatus;
import com.smartmall.tlai.business.order.repository.OrderRepository;
import com.smartmall.tlai.business.product.domain.Product;
import com.smartmall.tlai.business.product.repository.ProductRepository;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock OrderRepository orderRepository;
    @Mock ProductRepository productRepository;
    @Mock CustomerRepository customerRepository;

    @Test
    void createUsesServerPriceAndMergesDuplicateProducts() {
        when(customerRepository.findById(7)).thenReturn(Optional.of(customer(7, "张三")));
        when(productRepository.findById(3)).thenReturn(Optional.of(product(3, "SM-003", "键盘", "99.90")));
        when(productRepository.reserveStock(3, 3)).thenReturn(true);
        when(orderRepository.insert(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = service().create(new CreateOrderCommand(7,
                List.of(new OrderItemCommand(3, 1), new OrderItemCommand(3, 2))));

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).insert(saved.capture());
        verify(productRepository).reserveStock(3, 3);
        assertThat(saved.getValue().items()).hasSize(1);
        assertThat(saved.getValue().items().getFirst().quantity()).isEqualTo(3);
        assertThat(result.totalAmount()).isEqualByComparingTo("299.70");
        assertThat(result.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void createRollsBackWhenAtomicStockReservationFails() {
        when(customerRepository.findById(7)).thenReturn(Optional.of(customer(7, "张三")));
        when(productRepository.findById(3)).thenReturn(Optional.of(product(3, "SM-003", "键盘", "99.90")));
        when(productRepository.reserveStock(3, 5)).thenReturn(false);

        assertThatThrownBy(() -> service().create(new CreateOrderCommand(7,
                List.of(new OrderItemCommand(3, 5)))))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_STOCK));
        verify(orderRepository, never()).insert(any());
    }

    @Test
    void payOnlyChangesPendingOrder() {
        Order pending = order(11, OrderStatus.PENDING_PAYMENT);
        Order paid = new Order(pending.id(), pending.orderNo(), pending.customerId(), pending.customerName(),
                pending.totalAmount(), OrderStatus.PAID, pending.items(), OffsetDateTime.now(ZoneOffset.UTC),
                null, null, pending.createdAt(), pending.updatedAt());
        when(orderRepository.findById(11)).thenReturn(Optional.of(pending), Optional.of(paid));
        when(orderRepository.markPaid(anyLong(), any(OffsetDateTime.class))).thenReturn(true);

        Order result = service().pay(11);

        assertThat(result.status()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).markPaid(anyLong(), any(OffsetDateTime.class));
    }

    @Test
    void cancelPendingOrderRestoresEveryItemStock() {
        Order pending = order(11, OrderStatus.PENDING_PAYMENT);
        Order cancelled = new Order(pending.id(), pending.orderNo(), pending.customerId(), pending.customerName(),
                pending.totalAmount(), OrderStatus.CANCELLED, pending.items(), null,
                OffsetDateTime.now(ZoneOffset.UTC), null, pending.createdAt(), pending.updatedAt());
        when(orderRepository.findById(11)).thenReturn(Optional.of(pending), Optional.of(cancelled));
        when(orderRepository.markCancelled(anyLong(), any(OffsetDateTime.class))).thenReturn(true);

        Order result = service().cancel(11);

        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(productRepository).releaseStock(3, 2);
    }

    @Test
    void paidOrderCannotBeCancelled() {
        when(orderRepository.findById(11)).thenReturn(Optional.of(order(11, OrderStatus.PAID)));

        assertThatThrownBy(() -> service().cancel(11))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ORDER_STATUS_INVALID));
        verify(orderRepository, never()).markCancelled(anyLong(), any(OffsetDateTime.class));
    }

    @Test
    void payReportsConflictWhenAnotherRequestChangedStatusFirst() {
        when(orderRepository.findById(11)).thenReturn(Optional.of(order(11, OrderStatus.PENDING_PAYMENT)));
        when(orderRepository.markPaid(anyLong(), any(OffsetDateTime.class))).thenReturn(false);

        assertThatThrownBy(() -> service().pay(11))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ORDER_STATUS_INVALID));
    }

    private OrderService service() {
        return new OrderService(orderRepository, productRepository, customerRepository);
    }

    private Customer customer(long id, String name) {
        return new Customer(id, name, "13800000000", null, null, null);
    }

    private Product product(long id, String sku, String name, String price) {
        return new Product(id, sku, name, null, new BigDecimal(price), 10, true, null, null);
    }

    private Order order(long id, OrderStatus status) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OrderItem item = new OrderItem(21L, 3L, "SM-003", "键盘",
                new BigDecimal("99.90"), 2, new BigDecimal("199.80"));
        return new Order(id, "SM202609240001", 7L, "张三", new BigDecimal("199.80"),
                status, List.of(item), null, null, null, now, now);
    }
}

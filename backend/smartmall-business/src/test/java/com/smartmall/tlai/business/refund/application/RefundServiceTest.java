package com.smartmall.tlai.business.refund.application;

import com.smartmall.tlai.business.order.domain.Order;
import com.smartmall.tlai.business.order.domain.OrderItem;
import com.smartmall.tlai.business.order.domain.OrderStatus;
import com.smartmall.tlai.business.order.repository.OrderRepository;
import com.smartmall.tlai.business.product.repository.ProductRepository;
import com.smartmall.tlai.business.refund.domain.RefundRequest;
import com.smartmall.tlai.business.refund.domain.RefundStatus;
import com.smartmall.tlai.business.refund.repository.RefundRequestRepository;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {
    @Mock RefundRequestRepository refundRepository;
    @Mock OrderRepository orderRepository;
    @Mock ProductRepository productRepository;

    @Test
    void createOnlyAcceptsPaidOrder() {
        when(orderRepository.findById(8)).thenReturn(Optional.of(order(OrderStatus.PENDING_PAYMENT)));

        assertThatThrownBy(() -> service().create(new CreateRefundCommand(8, 1, "operator", "不需要了")))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> org.assertj.core.api.Assertions.assertThat(error.getErrorCode())
                                .isEqualTo(ErrorCode.ORDER_STATUS_INVALID));
        verify(refundRepository, never()).insert(any());
    }

    @Test
    void approveChangesBothStatusesAndRestoresStockOnce() {
        RefundRequest pending = refund(RefundStatus.PENDING_REVIEW);
        RefundRequest completed = refund(RefundStatus.COMPLETED);
        when(refundRepository.findById(5)).thenReturn(Optional.of(pending), Optional.of(completed));
        when(orderRepository.findById(8)).thenReturn(Optional.of(order(OrderStatus.REFUNDING)));
        when(orderRepository.markRefunded(anyLong(), any(OffsetDateTime.class))).thenReturn(true);
        when(refundRepository.markCompleted(anyLong(), anyLong(), any(), any(), any(OffsetDateTime.class)))
                .thenReturn(true);

        service().approve(5, new ReviewRefundCommand(2, "admin", "审核通过"));

        verify(productRepository).releaseStock(3, 2);
        verify(orderRepository).markRefunded(anyLong(), any(OffsetDateTime.class));
    }

    @Test
    void rejectReturnsOrderToPaidWithoutRestoringStock() {
        RefundRequest pending = refund(RefundStatus.PENDING_REVIEW);
        RefundRequest rejected = refund(RefundStatus.REJECTED);
        when(refundRepository.findById(5)).thenReturn(Optional.of(pending), Optional.of(rejected));
        when(orderRepository.restorePaidFromRefunding(anyLong(), any(OffsetDateTime.class))).thenReturn(true);
        when(refundRepository.markRejected(anyLong(), anyLong(), any(), any(), any(OffsetDateTime.class)))
                .thenReturn(true);

        service().reject(5, new ReviewRefundCommand(2, "admin", "不符合条件"));

        verify(productRepository, never()).releaseStock(anyLong(), anyInt());
    }

    private RefundService service() {
        return new RefundService(refundRepository, orderRepository, productRepository);
    }

    private Order order(OrderStatus status) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OrderItem item = new OrderItem(10L, 3L, "SKU-3", "机械键盘", new BigDecimal("199.00"),
                2, new BigDecimal("398.00"));
        return new Order(8L, "SM202609240001", 7L, "张三", new BigDecimal("398.00"), status,
                List.of(item), now, null, null, now, now);
    }

    private RefundRequest refund(RefundStatus status) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new RefundRequest(5L, "RF202609240001", 8L, "SM202609240001",
                new BigDecimal("398.00"), "不需要了", status, 1L, "operator",
                null, null, null, now, null, now, now);
    }
}

package com.smartmall.tlai.business.refund.application;

import com.smartmall.tlai.business.order.domain.Order;
import com.smartmall.tlai.business.order.domain.OrderItem;
import com.smartmall.tlai.business.order.domain.OrderStatus;
import com.smartmall.tlai.business.order.repository.OrderRepository;
import com.smartmall.tlai.business.product.repository.ProductRepository;
import com.smartmall.tlai.business.refund.domain.RefundRequest;
import com.smartmall.tlai.business.refund.domain.RefundStatus;
import com.smartmall.tlai.business.refund.repository.RefundRequestRepository;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class RefundService {
    private static final DateTimeFormatter NUMBER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RefundRequestRepository refundRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public RefundService(RefundRequestRepository refundRepository, OrderRepository orderRepository,
                         ProductRepository productRepository) {
        this.refundRepository = refundRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<RefundRequest> list(int page, int size, String keyword, String statusValue) {
        String search = keyword == null ? "" : keyword.trim();
        RefundStatus status = parseStatus(statusValue);
        return new PageResponse<>(refundRepository.findPage(page, size, search, status),
                refundRepository.count(search, status), page, size);
    }

    @Transactional(readOnly = true)
    public RefundRequest get(long id) {
        return requireRefund(id);
    }

    @Transactional
    public RefundRequest create(CreateRefundCommand command) {
        Order order = requireOrder(command.orderId());
        if (order.status() != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "只有已支付订单可以申请退款");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        // 先用条件更新抢占订单状态；并发提交时只有一个请求能从 PAID 进入 REFUNDING。
        if (!orderRepository.markRefunding(order.id(), now)) {
            throw new BusinessException(ErrorCode.REFUND_ALREADY_EXISTS);
        }
        RefundRequest refund = new RefundRequest(null, createRefundNo(now), order.id(), order.orderNo(),
                order.totalAmount(), command.reason().trim(), RefundStatus.PENDING_REVIEW,
                command.applicantId(), command.applicantName(), null, null, null,
                now, null, now, now);
        return refundRepository.insert(refund);
    }

    @Transactional
    public RefundRequest approve(long id, ReviewRefundCommand command) {
        RefundRequest refund = requirePendingRefund(id);
        Order order = requireOrder(refund.orderId());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // 订单、退款单和库存处于同一事务；其中任一步失败，数据库会整体回滚。
        if (!orderRepository.markRefunded(order.id(), now)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        if (!refundRepository.markCompleted(id, command.reviewerId(), command.reviewerName(),
                normalizeOptional(command.remark()), now)) {
            throw new BusinessException(ErrorCode.REFUND_STATUS_INVALID);
        }
        for (OrderItem item : order.items()) {
            productRepository.releaseStock(item.productId(), item.quantity());
        }
        return requireRefund(id);
    }

    @Transactional
    public RefundRequest reject(long id, ReviewRefundCommand command) {
        RefundRequest refund = requirePendingRefund(id);
        String remark = normalizeOptional(command.remark());
        if (remark == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "驳回退款时必须填写原因");
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (!orderRepository.restorePaidFromRefunding(refund.orderId(), now)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        if (!refundRepository.markRejected(id, command.reviewerId(), command.reviewerName(), remark, now)) {
            throw new BusinessException(ErrorCode.REFUND_STATUS_INVALID);
        }
        return requireRefund(id);
    }

    private RefundRequest requireRefund(long id) {
        return refundRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));
    }

    private RefundRequest requirePendingRefund(long id) {
        RefundRequest refund = requireRefund(id);
        if (refund.status() != RefundStatus.PENDING_REVIEW) {
            throw new BusinessException(ErrorCode.REFUND_STATUS_INVALID);
        }
        return refund;
    }

    private Order requireOrder(long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    private RefundStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return RefundStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "退款状态筛选值不正确");
        }
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String createRefundNo(OffsetDateTime now) {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        return "RF" + NUMBER_TIME_FORMAT.format(now) + random;
    }
}

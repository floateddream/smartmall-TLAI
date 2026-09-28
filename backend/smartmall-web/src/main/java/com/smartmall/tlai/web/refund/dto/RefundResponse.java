package com.smartmall.tlai.web.refund.dto;

import com.smartmall.tlai.business.refund.domain.RefundRequest;
import com.smartmall.tlai.business.refund.domain.RefundStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RefundResponse(Long id, String refundNo, Long orderId, String orderNo,
                             BigDecimal amount, String reason, RefundStatus status,
                             Long applicantId, String applicantName,
                             Long reviewerId, String reviewerName, String reviewRemark,
                             OffsetDateTime appliedAt, OffsetDateTime reviewedAt,
                             OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static RefundResponse from(RefundRequest refund) {
        return new RefundResponse(refund.id(), refund.refundNo(), refund.orderId(), refund.orderNo(),
                refund.amount(), refund.reason(), refund.status(), refund.applicantId(), refund.applicantName(),
                refund.reviewerId(), refund.reviewerName(), refund.reviewRemark(), refund.appliedAt(),
                refund.reviewedAt(), refund.createdAt(), refund.updatedAt());
    }
}

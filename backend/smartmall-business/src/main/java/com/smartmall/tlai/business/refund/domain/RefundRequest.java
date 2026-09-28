package com.smartmall.tlai.business.refund.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RefundRequest(Long id, String refundNo, Long orderId, String orderNo,
                            BigDecimal amount, String reason, RefundStatus status,
                            Long applicantId, String applicantName,
                            Long reviewerId, String reviewerName, String reviewRemark,
                            OffsetDateTime appliedAt, OffsetDateTime reviewedAt,
                            OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}

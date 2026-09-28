package com.smartmall.tlai.business.refund.repository;

import com.smartmall.tlai.business.refund.domain.RefundRequest;
import com.smartmall.tlai.business.refund.domain.RefundStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface RefundRequestRepository {
    long count(String keyword, RefundStatus status);
    List<RefundRequest> findPage(int page, int size, String keyword, RefundStatus status);
    Optional<RefundRequest> findById(long id);
    RefundRequest insert(RefundRequest refund);
    boolean markCompleted(long id, long reviewerId, String reviewerName, String remark, OffsetDateTime changedAt);
    boolean markRejected(long id, long reviewerId, String reviewerName, String remark, OffsetDateTime changedAt);
}

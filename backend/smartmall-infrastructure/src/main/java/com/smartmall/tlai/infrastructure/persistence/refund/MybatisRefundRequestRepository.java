package com.smartmall.tlai.infrastructure.persistence.refund;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartmall.tlai.business.refund.domain.RefundRequest;
import com.smartmall.tlai.business.refund.domain.RefundStatus;
import com.smartmall.tlai.business.refund.repository.RefundRequestRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class MybatisRefundRequestRepository implements RefundRequestRepository {
    private final RefundRequestMapper mapper;

    public MybatisRefundRequestRepository(RefundRequestMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public long count(String keyword, RefundStatus status) {
        return mapper.selectCount(filter(keyword, status));
    }

    @Override
    public List<RefundRequest> findPage(int page, int size, String keyword, RefundStatus status) {
        long offset = ((long) page - 1) * size;
        return mapper.selectList(filter(keyword, status)
                        .orderByDesc(RefundRequestPersistenceEntity::getId)
                        .last("LIMIT " + size + " OFFSET " + offset))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<RefundRequest> findById(long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public RefundRequest insert(RefundRequest refund) {
        RefundRequestPersistenceEntity entity = toEntity(refund);
        mapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public boolean markCompleted(long id, long reviewerId, String reviewerName,
                                 String remark, OffsetDateTime changedAt) {
        return mapper.review(id, RefundStatus.COMPLETED.name(), reviewerId, reviewerName, remark, changedAt) == 1;
    }

    @Override
    public boolean markRejected(long id, long reviewerId, String reviewerName,
                                String remark, OffsetDateTime changedAt) {
        return mapper.review(id, RefundStatus.REJECTED.name(), reviewerId, reviewerName, remark, changedAt) == 1;
    }

    private LambdaQueryWrapper<RefundRequestPersistenceEntity> filter(String keyword, RefundStatus status) {
        LambdaQueryWrapper<RefundRequestPersistenceEntity> query = Wrappers.lambdaQuery(RefundRequestPersistenceEntity.class);
        if (!keyword.isEmpty()) {
            query.and(part -> part.like(RefundRequestPersistenceEntity::getRefundNo, keyword.toUpperCase(Locale.ROOT))
                    .or().like(RefundRequestPersistenceEntity::getOrderNo, keyword.toUpperCase(Locale.ROOT))
                    .or().like(RefundRequestPersistenceEntity::getApplicantName, keyword));
        }
        if (status != null) query.eq(RefundRequestPersistenceEntity::getStatus, status.name());
        return query;
    }

    private RefundRequest toDomain(RefundRequestPersistenceEntity entity) {
        return new RefundRequest(entity.getId(), entity.getRefundNo(), entity.getOrderId(), entity.getOrderNo(),
                entity.getAmount(), entity.getReason(), RefundStatus.valueOf(entity.getStatus()),
                entity.getApplicantId(), entity.getApplicantName(), entity.getReviewerId(), entity.getReviewerName(),
                entity.getReviewRemark(), entity.getAppliedAt(), entity.getReviewedAt(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private RefundRequestPersistenceEntity toEntity(RefundRequest refund) {
        RefundRequestPersistenceEntity entity = new RefundRequestPersistenceEntity();
        entity.setId(refund.id());
        entity.setRefundNo(refund.refundNo());
        entity.setOrderId(refund.orderId());
        entity.setOrderNo(refund.orderNo());
        entity.setAmount(refund.amount());
        entity.setReason(refund.reason());
        entity.setStatus(refund.status().name());
        entity.setApplicantId(refund.applicantId());
        entity.setApplicantName(refund.applicantName());
        entity.setReviewerId(refund.reviewerId());
        entity.setReviewerName(refund.reviewerName());
        entity.setReviewRemark(refund.reviewRemark());
        entity.setAppliedAt(refund.appliedAt());
        entity.setReviewedAt(refund.reviewedAt());
        entity.setCreatedAt(refund.createdAt());
        entity.setUpdatedAt(refund.updatedAt());
        return entity;
    }
}

package com.smartmall.tlai.infrastructure.persistence.refund;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;

@Mapper
public interface RefundRequestMapper extends BaseMapper<RefundRequestPersistenceEntity> {
    @Update("""
            UPDATE refund_request
            SET status = #{targetStatus}, reviewer_id = #{reviewerId}, reviewer_name = #{reviewerName},
                review_remark = #{remark}, reviewed_at = #{changedAt}, updated_at = #{changedAt}
            WHERE id = #{id} AND status = 'PENDING_REVIEW'
            """)
    int review(@Param("id") long id, @Param("targetStatus") String targetStatus,
               @Param("reviewerId") long reviewerId, @Param("reviewerName") String reviewerName,
               @Param("remark") String remark, @Param("changedAt") OffsetDateTime changedAt);
}

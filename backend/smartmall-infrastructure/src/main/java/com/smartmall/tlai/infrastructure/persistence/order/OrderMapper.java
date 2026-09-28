package com.smartmall.tlai.infrastructure.persistence.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;

@Mapper
public interface OrderMapper extends BaseMapper<OrderPersistenceEntity> {
    @Update("""
            UPDATE orders
            SET status = 'PAID', paid_at = #{changedAt}, updated_at = #{changedAt}
            WHERE id = #{id} AND status = 'PENDING_PAYMENT'
            """)
    int markPaid(@Param("id") long id, @Param("changedAt") OffsetDateTime changedAt);

    @Update("""
            UPDATE orders
            SET status = 'CANCELLED', cancelled_at = #{changedAt}, updated_at = #{changedAt}
            WHERE id = #{id} AND status = 'PENDING_PAYMENT'
            """)
    int markCancelled(@Param("id") long id, @Param("changedAt") OffsetDateTime changedAt);

    @Update("""
            UPDATE orders
            SET status = 'REFUNDING', updated_at = #{changedAt}
            WHERE id = #{id} AND status = 'PAID'
            """)
    int markRefunding(@Param("id") long id, @Param("changedAt") OffsetDateTime changedAt);

    @Update("""
            UPDATE orders
            SET status = 'REFUNDED', refunded_at = #{changedAt}, updated_at = #{changedAt}
            WHERE id = #{id} AND status = 'REFUNDING'
            """)
    int markRefunded(@Param("id") long id, @Param("changedAt") OffsetDateTime changedAt);

    @Update("""
            UPDATE orders
            SET status = 'PAID', updated_at = #{changedAt}
            WHERE id = #{id} AND status = 'REFUNDING'
            """)
    int restorePaidFromRefunding(@Param("id") long id, @Param("changedAt") OffsetDateTime changedAt);
}

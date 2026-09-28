package com.smartmall.tlai.infrastructure.persistence.ticket;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;

@Mapper
public interface TicketMapper extends BaseMapper<TicketPersistenceEntity> {
    @Update("""
            UPDATE after_sales_ticket
            SET status = #{targetStatus},
                resolved_at = CASE WHEN #{targetStatus} = 'RESOLVED' THEN #{changedAt} ELSE resolved_at END,
                closed_at = CASE WHEN #{targetStatus} = 'CLOSED' THEN #{changedAt} ELSE closed_at END,
                updated_at = #{changedAt}
            WHERE id = #{id} AND status = #{expectedStatus}
            """)
    int transitionStatus(@Param("id") long id, @Param("expectedStatus") String expectedStatus,
                         @Param("targetStatus") String targetStatus,
                         @Param("changedAt") OffsetDateTime changedAt);

    @Update("UPDATE after_sales_ticket SET updated_at = #{changedAt} WHERE id = #{id}")
    int touch(@Param("id") long id, @Param("changedAt") OffsetDateTime changedAt);
}

package com.smartmall.tlai.infrastructure.persistence.product;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<ProductPersistenceEntity> {
    /**
     * 将库存判断和扣减放进一条 SQL，两个请求并发购买最后一件商品时只有一个会成功。
     */
    @Update("""
            UPDATE product
            SET stock = stock - #{quantity}, updated_at = CURRENT_TIMESTAMP
            WHERE id = #{productId} AND active = TRUE AND stock >= #{quantity}
            """)
    int reserveStock(@Param("productId") long productId, @Param("quantity") int quantity);

    @Update("""
            UPDATE product
            SET stock = stock + #{quantity}, updated_at = CURRENT_TIMESTAMP
            WHERE id = #{productId}
            """)
    int releaseStock(@Param("productId") long productId, @Param("quantity") int quantity);
}

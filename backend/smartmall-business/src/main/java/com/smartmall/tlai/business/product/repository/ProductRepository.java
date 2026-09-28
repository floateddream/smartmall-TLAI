package com.smartmall.tlai.business.product.repository;

import com.smartmall.tlai.business.product.domain.Product;
import java.util.List;
import java.util.Optional;

/** 商品持久化端口，具体 MyBatis 实现在 infrastructure 模块。 */
public interface ProductRepository {
    long count(String keyword);
    List<Product> findPage(int page, int size, String keyword);
    Optional<Product> findById(long id);
    boolean existsBySku(String sku, Long excludedId);
    Product insert(Product product);
    Product update(Product product);

    /** 只有商品已上架且库存充足时才扣减，返回 false 表示条件不满足。 */
    boolean reserveStock(long productId, int quantity);

    /** 取消未支付订单时归还此前预占的库存。 */
    void releaseStock(long productId, int quantity);
}

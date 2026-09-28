package com.smartmall.tlai.infrastructure.persistence.product;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartmall.tlai.business.product.domain.Product;
import com.smartmall.tlai.business.product.repository.ProductRepository;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class MybatisProductRepository implements ProductRepository {
    private final ProductMapper mapper;

    public MybatisProductRepository(ProductMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public long count(String keyword) {
        return mapper.selectCount(filter(keyword));
    }

    @Override
    public List<Product> findPage(int page, int size, String keyword) {
        long offset = ((long) page - 1) * size;
        LambdaQueryWrapper<ProductPersistenceEntity> query = filter(keyword)
                .orderByDesc(ProductPersistenceEntity::getId)
                .last("LIMIT " + size + " OFFSET " + offset);
        return mapper.selectList(query).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Product> findById(long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public boolean existsBySku(String sku, Long excludedId) {
        LambdaQueryWrapper<ProductPersistenceEntity> query = Wrappers.<ProductPersistenceEntity>lambdaQuery()
                .eq(ProductPersistenceEntity::getSku, sku);
        if (excludedId != null) query.ne(ProductPersistenceEntity::getId, excludedId);
        return mapper.selectCount(query) > 0;
    }

    @Override
    public Product insert(Product product) {
        ProductPersistenceEntity entity = toEntity(product);
        try {
            mapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.PRODUCT_SKU_EXISTS);
        }
        return toDomain(entity);
    }

    @Override
    public Product update(Product product) {
        ProductPersistenceEntity entity = toEntity(product);
        try {
            mapper.updateById(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.PRODUCT_SKU_EXISTS);
        }
        return toDomain(entity);
    }

    @Override
    public boolean reserveStock(long productId, int quantity) {
        return mapper.reserveStock(productId, quantity) == 1;
    }

    @Override
    public void releaseStock(long productId, int quantity) {
        if (mapper.releaseStock(productId, quantity) != 1) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    private LambdaQueryWrapper<ProductPersistenceEntity> filter(String keyword) {
        LambdaQueryWrapper<ProductPersistenceEntity> query = Wrappers.lambdaQuery(ProductPersistenceEntity.class);
        if (!keyword.isEmpty()) {
            query.and(part -> part.like(ProductPersistenceEntity::getSku, keyword.toUpperCase(Locale.ROOT))
                    .or().like(ProductPersistenceEntity::getName, keyword));
        }
        return query;
    }

    private Product toDomain(ProductPersistenceEntity entity) {
        return new Product(entity.getId(), entity.getSku(), entity.getName(), entity.getDescription(),
                entity.getPrice(), entity.getStock(), Boolean.TRUE.equals(entity.getActive()),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private ProductPersistenceEntity toEntity(Product product) {
        ProductPersistenceEntity entity = new ProductPersistenceEntity();
        entity.setId(product.id());
        entity.setSku(product.sku());
        entity.setName(product.name());
        entity.setDescription(product.description());
        entity.setPrice(product.price());
        entity.setStock(product.stock());
        entity.setActive(product.active());
        entity.setCreatedAt(product.createdAt());
        entity.setUpdatedAt(product.updatedAt());
        return entity;
    }
}

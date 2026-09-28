package com.smartmall.tlai.business.product.application;

import com.smartmall.tlai.business.product.domain.Product;
import com.smartmall.tlai.business.product.repository.ProductRepository;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
public class ProductService {
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<Product> list(int page, int size, String keyword) {
        String search = keyword == null ? "" : keyword.trim();
        return new PageResponse<>(repository.findPage(page, size, search),
                repository.count(search), page, size);
    }

    @Transactional(readOnly = true)
    public Product get(long id) {
        return requireProduct(id);
    }

    @Transactional
    public Product create(ProductCommand command) {
        String sku = normalizeSku(command.sku());
        ensureSkuAvailable(sku, null);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Product product = new Product(null, sku, command.name().trim(), normalizeDescription(command.description()),
                command.price(), command.stock(), command.active(), now, now);
        return repository.insert(product);
    }

    @Transactional
    public Product update(long id, ProductCommand command) {
        Product existing = requireProduct(id);
        String sku = normalizeSku(command.sku());
        ensureSkuAvailable(sku, id);
        Product product = new Product(id, sku, command.name().trim(), normalizeDescription(command.description()),
                command.price(), command.stock(), command.active(), existing.createdAt(),
                OffsetDateTime.now(ZoneOffset.UTC));
        return repository.update(product);
    }

    private Product requireProduct(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private void ensureSkuAvailable(String sku, Long excludedId) {
        if (repository.existsBySku(sku, excludedId)) {
            throw new BusinessException(ErrorCode.PRODUCT_SKU_EXISTS);
        }
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}

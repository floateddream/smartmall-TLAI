package com.smartmall.tlai.business.product.application;

import com.smartmall.tlai.business.product.domain.Product;
import com.smartmall.tlai.business.product.repository.ProductRepository;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock ProductRepository repository;

    @Test
    void createNormalizesSkuAndOptionalDescription() {
        ProductCommand input = new ProductCommand(" sm-001 ", "商品 A", "  ",
                new BigDecimal("19.90"), 5, true);

        new ProductService(repository).create(input);

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(repository).insert(saved.capture());
        assertThat(saved.getValue().sku()).isEqualTo("SM-001");
        assertThat(saved.getValue().description()).isNull();
        assertThat(saved.getValue().stock()).isEqualTo(5);
    }

    @Test
    void rejectsDuplicateSku() {
        when(repository.existsBySku("SM-001", null)).thenReturn(true);
        ProductCommand input = new ProductCommand("SM-001", "商品 A", null,
                new BigDecimal("19.90"), 5, true);

        assertThatThrownBy(() -> new ProductService(repository).create(input))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_SKU_EXISTS));
    }
}

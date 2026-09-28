package com.smartmall.tlai.business.product.application;

import java.math.BigDecimal;

/** Web 等入口层向商品业务提交的结构化命令。 */
public record ProductCommand(String sku, String name, String description,
                             BigDecimal price, int stock, boolean active) {
}

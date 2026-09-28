package com.smartmall.tlai.business.customer.domain;

import java.time.OffsetDateTime;

/** 客户是订单业务对象，不是可以登录后台的系统用户。 */
public record Customer(Long id, String name, String phone, String email,
                       OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}

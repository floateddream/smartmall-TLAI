package com.smartmall.tlai.business.access.domain;

import java.time.OffsetDateTime;

/** 数据库中的系统级配置，不与具体角色的启停状态耦合。 */
public record SystemSetting(String key, String value, Long updatedBy, OffsetDateTime updatedAt) {
}

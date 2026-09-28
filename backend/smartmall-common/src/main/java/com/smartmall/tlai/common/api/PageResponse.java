package com.smartmall.tlai.common.api;

import java.util.List;

/** 分页从第 1 页开始，total 是过滤条件下的总记录数。 */
public record PageResponse<T>(List<T> items, long total, int page, int size) {
}

package com.smartmall.tlai.web.refund.dto;

import jakarta.validation.constraints.Size;

public record ReviewRefundRequest(
        @Size(max = 500, message = "审核备注不能超过 500 个字符")
        String remark) {
}

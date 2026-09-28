package com.smartmall.tlai.web.ticket.dto;

import com.smartmall.tlai.business.ticket.domain.TicketCategory;
import com.smartmall.tlai.business.ticket.domain.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @Positive(message = "订单 ID 必须大于 0") long orderId,
        @NotNull(message = "工单分类不能为空") TicketCategory category,
        @NotNull(message = "优先级不能为空") TicketPriority priority,
        @NotBlank(message = "工单主题不能为空")
        @Size(max = 120, message = "工单主题不能超过 120 个字符") String subject,
        @NotBlank(message = "问题描述不能为空")
        @Size(max = 2000, message = "问题描述不能超过 2000 个字符") String description) {
}

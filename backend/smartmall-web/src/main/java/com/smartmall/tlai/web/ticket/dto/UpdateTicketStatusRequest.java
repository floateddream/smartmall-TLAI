package com.smartmall.tlai.web.ticket.dto;

import com.smartmall.tlai.business.ticket.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTicketStatusRequest(
        @NotNull(message = "目标状态不能为空") TicketStatus status,
        @Size(max = 1000, message = "处理说明不能超过 1000 个字符") String remark) {
}

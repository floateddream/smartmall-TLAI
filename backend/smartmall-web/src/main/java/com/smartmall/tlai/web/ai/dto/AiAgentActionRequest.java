package com.smartmall.tlai.web.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiAgentActionRequest(
        @NotBlank(message = "确认令牌不能为空")
        @Size(max = 128, message = "确认令牌格式不正确") String token) {
}

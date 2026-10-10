package com.smartmall.tlai.web.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiConversationMessageRequest(
        @NotBlank @Size(max = 4000) String content
) {
}

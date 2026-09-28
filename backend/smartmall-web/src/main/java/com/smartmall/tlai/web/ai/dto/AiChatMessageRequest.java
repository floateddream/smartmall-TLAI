package com.smartmall.tlai.web.ai.dto;

import com.smartmall.tlai.ai.model.AiChatMessage;
import com.smartmall.tlai.ai.model.AiMessageRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiChatMessageRequest(
        @NotNull AiMessageRole role,
        @NotBlank @Size(max = 4000) String content
) {
    public AiChatMessage toDomain() {
        return new AiChatMessage(role, content);
    }
}

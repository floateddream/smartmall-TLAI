package com.smartmall.tlai.web.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AiChatRequest(
        @NotEmpty @Size(max = 30) List<@Valid AiChatMessageRequest> messages,
        boolean knowledgeEnabled,
        boolean agentEnabled
) {
}

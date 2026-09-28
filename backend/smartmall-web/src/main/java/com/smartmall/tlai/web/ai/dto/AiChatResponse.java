package com.smartmall.tlai.web.ai.dto;

import com.smartmall.tlai.ai.model.AiChatResult;

public record AiChatResponse(String content, String modelName, long latencyMs) {
    public static AiChatResponse from(AiChatResult result) {
        return new AiChatResponse(result.content(), result.modelName(), result.latencyMs());
    }
}

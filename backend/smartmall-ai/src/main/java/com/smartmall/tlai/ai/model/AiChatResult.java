package com.smartmall.tlai.ai.model;

public record AiChatResult(String content, String modelName, long latencyMs) {
}

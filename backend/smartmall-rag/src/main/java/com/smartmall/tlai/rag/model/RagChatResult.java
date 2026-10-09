package com.smartmall.tlai.rag.model;

import java.util.List;

public record RagChatResult(
        String content,
        String modelName,
        long latencyMs,
        List<RagSource> sources
) {
}

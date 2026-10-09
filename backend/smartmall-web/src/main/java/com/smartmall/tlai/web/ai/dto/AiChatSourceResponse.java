package com.smartmall.tlai.web.ai.dto;

import com.smartmall.tlai.rag.model.RagSource;

public record AiChatSourceResponse(
        int reference,
        long documentId,
        String documentName,
        int chunkIndex,
        String excerpt,
        double score
) {
    public static AiChatSourceResponse from(RagSource source) {
        return new AiChatSourceResponse(source.reference(), source.documentId(), source.documentName(),
                source.chunkIndex(), source.excerpt(), source.score());
    }
}

package com.smartmall.tlai.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "smartmall.rag")
public record RagProperties(
        @DefaultValue("5") int topK,
        @DefaultValue("0.35") double minScore,
        @DefaultValue("10000") int maxContextCharacters
) {
    public RagProperties {
        if (topK < 1 || topK > 10) throw new IllegalArgumentException("RAG top-k must be between 1 and 10");
        if (minScore < -1 || minScore > 1) throw new IllegalArgumentException("RAG min-score must be between -1 and 1");
        if (maxContextCharacters < 1000 || maxContextCharacters > 15000) {
            throw new IllegalArgumentException("RAG max context characters must be between 1000 and 15000");
        }
    }
}

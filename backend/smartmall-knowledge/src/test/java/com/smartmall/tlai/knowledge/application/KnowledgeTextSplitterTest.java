package com.smartmall.tlai.knowledge.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeTextSplitterTest {
    private final KnowledgeTextSplitter splitter = new KnowledgeTextSplitter();

    @Test
    void keepsShortDocumentInOneChunk() {
        var chunks = splitter.split("第一段内容。\n\n第二段内容。");

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().content()).contains("第一段内容", "第二段内容");
        assertThat(chunks.getFirst().tokenCount()).isPositive();
    }

    @Test
    void splitsLongDocumentWithOverlap() {
        String source = "退款规则用于说明审核要求。".repeat(150);

        var chunks = splitter.split(source);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.content().length())
                        .isLessThanOrEqualTo(KnowledgeTextSplitter.MAX_CHUNK_CHARACTERS));
        assertThat(chunks.get(1).content())
                .startsWith(chunks.getFirst().content()
                        .substring(chunks.getFirst().content().length()
                                - KnowledgeTextSplitter.CHUNK_OVERLAP_CHARACTERS));
    }
}

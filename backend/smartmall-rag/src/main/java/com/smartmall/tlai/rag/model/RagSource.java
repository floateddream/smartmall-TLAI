package com.smartmall.tlai.rag.model;

/** 返回给页面的引用来源，reference 对应回答提示词中的资料编号。 */
public record RagSource(
        int reference,
        long documentId,
        String documentName,
        int chunkIndex,
        String excerpt,
        double score
) {
}

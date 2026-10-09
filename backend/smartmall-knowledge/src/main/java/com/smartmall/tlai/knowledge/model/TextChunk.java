package com.smartmall.tlai.knowledge.model;

/** 文档分段的中间结果，tokenCount 是不依赖具体模型分词器的保守估算值。 */
public record TextChunk(int index, String content, int tokenCount) {
}

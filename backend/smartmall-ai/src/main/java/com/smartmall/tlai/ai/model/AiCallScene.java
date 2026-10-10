package com.smartmall.tlai.ai.model;

/** AI 调用的业务来源，只记录分类，不记录提示词或业务结果。 */
public enum AiCallScene {
    ASSISTANT_CHAT,
    KNOWLEDGE_RAG,
    INTERNAL_ORCHESTRATION,
    KNOWLEDGE_EMBEDDING,
    MODEL_CONNECTIVITY_TEST
}

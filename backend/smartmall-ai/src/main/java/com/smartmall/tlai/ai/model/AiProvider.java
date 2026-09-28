package com.smartmall.tlai.ai.model;

import java.util.List;
import java.util.Set;

/**
 * 后台可选的模型供应商预设。模型 ID 仍允许自定义，避免供应商发布新模型后必须升级系统。
 */
public enum AiProvider {
    OPENAI("OpenAI", "https://api.openai.com/v1",
            Set.of(AiModelType.CHAT, AiModelType.EMBEDDING),
            List.of("gpt-4.1-mini", "gpt-4.1", "gpt-4o-mini"),
            List.of("text-embedding-3-small", "text-embedding-3-large"), true),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1",
            Set.of(AiModelType.CHAT), List.of("deepseek-chat", "deepseek-reasoner"), List.of(), true),
    QWEN("通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1",
            Set.of(AiModelType.CHAT, AiModelType.EMBEDDING),
            List.of("qwen-turbo", "qwen-plus", "qwen-max"), List.of("text-embedding-v4"), true),
    MOONSHOT("Moonshot / Kimi", "https://api.moonshot.cn/v1",
            Set.of(AiModelType.CHAT),
            List.of("moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k"), List.of(), true),
    ZHIPU("智谱 GLM", "https://open.bigmodel.cn/api/paas/v4",
            Set.of(AiModelType.CHAT, AiModelType.EMBEDDING),
            List.of("glm-4-flash", "glm-4-air", "glm-4-plus"), List.of("embedding-3"), true),
    OLLAMA("Ollama", "http://localhost:11434/v1",
            Set.of(AiModelType.EMBEDDING), List.of(), List.of("bge-m3"), false),
    OPENAI_COMPATIBLE("自定义兼容接口", "",
            Set.of(AiModelType.CHAT, AiModelType.EMBEDDING), List.of(), List.of(), true);

    private final String displayName;
    private final String defaultBaseUrl;
    private final Set<AiModelType> supportedTypes;
    private final List<String> chatModels;
    private final List<String> embeddingModels;
    private final boolean credentialRequired;

    AiProvider(String displayName, String defaultBaseUrl, Set<AiModelType> supportedTypes,
               List<String> chatModels, List<String> embeddingModels, boolean credentialRequired) {
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
        this.supportedTypes = supportedTypes;
        this.chatModels = chatModels;
        this.embeddingModels = embeddingModels;
        this.credentialRequired = credentialRequired;
    }

    public String displayName() {
        return displayName;
    }

    public String defaultBaseUrl() {
        return defaultBaseUrl;
    }

    public Set<AiModelType> supportedTypes() {
        return supportedTypes;
    }

    public List<String> models(AiModelType modelType) {
        return modelType == AiModelType.EMBEDDING ? embeddingModels : chatModels;
    }

    public boolean supports(AiModelType modelType) {
        return supportedTypes.contains(modelType);
    }

    public boolean credentialRequired() {
        return credentialRequired;
    }
}

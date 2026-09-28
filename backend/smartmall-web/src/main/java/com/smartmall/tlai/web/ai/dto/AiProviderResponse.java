package com.smartmall.tlai.web.ai.dto;

import com.smartmall.tlai.ai.model.AiProvider;

import java.util.List;

public record AiProviderResponse(
        String code,
        String name,
        String defaultBaseUrl,
        List<String> supportedTypes,
        List<String> chatModels,
        List<String> embeddingModels,
        boolean credentialRequired
) {
    public static AiProviderResponse from(AiProvider provider) {
        return new AiProviderResponse(provider.name(), provider.displayName(),
                provider.defaultBaseUrl(), provider.supportedTypes().stream().map(Enum::name).sorted().toList(),
                provider.models(com.smartmall.tlai.ai.model.AiModelType.CHAT),
                provider.models(com.smartmall.tlai.ai.model.AiModelType.EMBEDDING),
                provider.credentialRequired());
    }
}

package com.smartmall.tlai.ai.gateway;

import com.smartmall.tlai.ai.model.AiChatMessage;
import com.smartmall.tlai.ai.model.AiChatResult;
import com.smartmall.tlai.ai.model.AiModelConfig;

import java.time.Duration;
import java.util.List;

public interface AiChatGateway {
    AiChatResult complete(AiModelConfig config, List<AiChatMessage> messages,
                          String systemPrompt, String apiKey, Duration timeout);
}

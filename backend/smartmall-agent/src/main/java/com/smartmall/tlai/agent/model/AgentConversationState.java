package com.smartmall.tlai.agent.model;

import java.util.List;

/** 仅保存受控意图、已知参数和缺失字段，不保存确认令牌或模型自由文本。 */
public record AgentConversationState(String intent, String argumentsJson,
                                     List<String> missingFields) {
    public AgentConversationState {
        argumentsJson = argumentsJson == null || argumentsJson.isBlank() ? "{}" : argumentsJson;
        missingFields = missingFields == null ? List.of() : List.copyOf(missingFields);
    }
}

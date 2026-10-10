package com.smartmall.tlai.web.ai.dto;

import com.smartmall.tlai.agent.model.AgentPageNavigation;

import java.util.Map;

public record AiAgentNavigationResponse(String path, Map<String, String> query) {
    public static AiAgentNavigationResponse from(AgentPageNavigation navigation) {
        return navigation == null ? null : new AiAgentNavigationResponse(navigation.path(), navigation.query());
    }
}

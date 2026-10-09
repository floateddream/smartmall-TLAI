package com.smartmall.tlai.web.ai.dto;

import com.smartmall.tlai.agent.model.AgentSuggestion;

public record AiSuggestionResponse(String label, String prompt, String capabilityCode) {
    public static AiSuggestionResponse from(AgentSuggestion suggestion) {
        return new AiSuggestionResponse(suggestion.label(), suggestion.prompt(), suggestion.capabilityCode());
    }
}

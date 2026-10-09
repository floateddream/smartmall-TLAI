package com.smartmall.tlai.web.ai.dto;

import com.smartmall.tlai.agent.model.AgentCapability;
import com.smartmall.tlai.agent.model.AgentCapabilityCategory;
import com.smartmall.tlai.agent.model.AgentCapabilityType;

import java.util.List;

public record AiCapabilityResponse(String code, String displayName, String description,
                                   AgentCapabilityCategory category, AgentCapabilityType type,
                                   List<String> examples) {
    public static AiCapabilityResponse from(AgentCapability capability) {
        return new AiCapabilityResponse(capability.code(), capability.displayName(), capability.description(),
                capability.category(), capability.type(), capability.examples());
    }
}

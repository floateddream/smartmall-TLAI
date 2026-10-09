package com.smartmall.tlai.agent.model;

import java.util.List;

public record AgentCapability(String code, String displayName, String description,
                              AgentCapabilityCategory category, AgentCapabilityType type,
                              String permission, List<String> examples,
                              String plannerInstruction) {
    public AgentCapability {
        examples = examples == null ? List.of() : List.copyOf(examples);
    }
}

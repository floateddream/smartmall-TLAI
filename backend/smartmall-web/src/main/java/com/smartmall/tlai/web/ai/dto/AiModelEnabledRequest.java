package com.smartmall.tlai.web.ai.dto;

import jakarta.validation.constraints.NotNull;

public record AiModelEnabledRequest(@NotNull Boolean enabled) {
}

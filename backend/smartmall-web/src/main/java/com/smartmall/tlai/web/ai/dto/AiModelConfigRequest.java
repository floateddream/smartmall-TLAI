package com.smartmall.tlai.web.ai.dto;

import com.smartmall.tlai.ai.model.AiProvider;
import com.smartmall.tlai.ai.model.AiModelType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AiModelConfigRequest(
        @NotBlank @Size(max = 80) String name,
        @NotNull AiModelType modelType,
        @NotNull AiProvider provider,
        @Size(max = 500) String baseUrl,
        @NotBlank @Size(max = 120) String modelName,
        @Size(max = 512) String apiKey,
        @NotNull @DecimalMin("0.0") @DecimalMax("2.0") BigDecimal temperature,
        @Min(128) @Max(32768) int maxTokens,
        @Min(1) @Max(4096) Integer embeddingDimensions
) {
}

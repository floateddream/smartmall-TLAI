package com.smartmall.tlai.web.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NaturalLanguageAnalyticsRequest(
        @NotBlank @Size(max = 500) String question
) {
}

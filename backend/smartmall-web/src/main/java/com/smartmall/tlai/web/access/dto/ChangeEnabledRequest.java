package com.smartmall.tlai.web.access.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeEnabledRequest(@NotNull Boolean enabled) {
}

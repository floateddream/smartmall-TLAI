package com.smartmall.tlai.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "smartmall.ai")
public record AiProperties(String masterKey, Duration requestTimeout) {
    public AiProperties {
        masterKey = masterKey == null ? "" : masterKey.trim();
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(60) : requestTimeout;
        if (requestTimeout.isNegative() || requestTimeout.isZero()) {
            throw new IllegalArgumentException("AI request timeout must be positive");
        }
    }

    public boolean masterKeyConfigured() {
        return !masterKey.isBlank();
    }
}

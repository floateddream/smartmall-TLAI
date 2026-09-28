package com.smartmall.tlai.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartmall.bootstrap-admin")
public record BootstrapAdminProperties(boolean enabled, String username, String email, String password) {
}

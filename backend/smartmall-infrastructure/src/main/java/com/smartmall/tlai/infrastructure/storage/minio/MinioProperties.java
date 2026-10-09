package com.smartmall.tlai.infrastructure.storage.minio;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartmall.storage.minio")
public record MinioProperties(String endpoint, String accessKey, String secretKey, String bucket) {
    public MinioProperties {
        endpoint = normalize(endpoint);
        accessKey = normalize(accessKey);
        secretKey = normalize(secretKey);
        bucket = bucket == null || bucket.isBlank() ? "smartmall-documents" : bucket.trim();
    }

    public boolean configured() {
        return !endpoint.isBlank() && !accessKey.isBlank() && !secretKey.isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

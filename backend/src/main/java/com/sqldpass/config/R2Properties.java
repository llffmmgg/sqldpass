package com.sqldpass.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Cloudflare R2 (S3 호환) 업로드 설정.
 * 환경변수가 비어있으면 R2UploadService 가 비활성 상태로 동작 (503 반환).
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "sqldpass.r2")
public class R2Properties {
    private String endpoint;
    private String bucket;
    private String accessKey;
    private String secretKey;
    private String publicBaseUrl;
    private long maxBytes = 5_242_880L;

    public boolean isEnabled() {
        return notBlank(endpoint) && notBlank(bucket) && notBlank(accessKey)
                && notBlank(secretKey) && notBlank(publicBaseUrl);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}

package com.sqldpass.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * 모의고사 PDF 생성기 설정.
 *
 * Playwright 헤드리스 Chromium 이 인쇄 전용 프론트 페이지를 렌더 → PDF 바이너리 → R2 캐시.
 *
 * - printPageBaseUrl: Chromium 이 GET 으로 열 프론트 인쇄 페이지의 베이스 (예: http://localhost:3000).
 *   인쇄 페이지는 같은 origin 의 /api/* 로 백엔드 호출 → nginx/Next.js rewrites 가 백엔드로 프록시.
 * - cacheKeyPrefix  : R2 오브젝트 키의 prefix
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "sqldpass.pdf")
public class PdfProperties {
    private String printPageBaseUrl = "http://localhost:3000";
    private String cacheKeyPrefix = "pdf/mock-exams";
    private boolean verifyOnStartup = false;
}

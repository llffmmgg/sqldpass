package com.sqldpass.controller.publicapi.dto;

import java.time.LocalDateTime;

/**
 * 공개(SSR/SEO) 게시판 응답 DTO. sitemap.ts 가 PUBLISHED 게시글 ID 와
 * lastModified 를 가져갈 때 사용.
 */
public final class PublicPostDtos {
    private PublicPostDtos() {}

    public record PublicPostSeoSummary(Long id, LocalDateTime updatedAt) {}
}

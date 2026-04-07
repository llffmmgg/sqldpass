package com.sqldpass.controller.publicapi.dto;

import java.util.List;

/**
 * 공개(로그인 불필요) 콘텐츠 API 응답 DTO 모음.
 * SEO 용도로 검색엔진에 노출되는 페이지가 사용한다.
 */
public final class PublicDtos {

    private PublicDtos() {}

    /** 자격증 메타 */
    public record PublicCertResponse(
            String slug,
            String name,
            String description,
            int questionCount,
            int categoryCount) {}

    /** 자격증별 카테고리 (leaf subject) */
    public record PublicCategoryResponse(
            long id,
            String name,
            String parentName,
            int questionCount) {}

    /** 카테고리 내 문제 리스트의 단건 */
    public record PublicQuestionSummary(
            long id,
            String contentPreview,
            String topic,
            Integer difficulty,
            String questionType) {}

    /** 문제 리스트 페이지 응답 */
    public record PublicQuestionPageResponse(
            List<PublicQuestionSummary> questions,
            int page,
            int size,
            long total,
            int totalPages) {}

    /** 문제 상세 — 정답/해설/키워드 모두 포함 (SEO 본문) */
    public record PublicQuestionDetailResponse(
            long id,
            String certSlug,
            String certName,
            long categoryId,
            String categoryName,
            String content,
            String questionType,
            Integer correctOption,
            String answer,
            List<String> keywords,
            String explanation,
            String topic,
            Integer difficulty) {}
}

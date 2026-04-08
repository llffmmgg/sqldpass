package com.sqldpass.controller.publicapi;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicCategoryResponse;
import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicCertResponse;
import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicQuestionDetailResponse;
import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicQuestionPageResponse;
import com.sqldpass.controller.publicapi.dto.PublicRankingResponse;
import com.sqldpass.controller.publicapi.dto.PublicStatsResponse;
import com.sqldpass.service.publicapi.PublicContentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 공개 콘텐츠 API — 로그인 불필요. 검색엔진 크롤링 및 SEO 유입 대상.
 * WebMvcConfig의 인터셉터 경로에 /api/public/** 이 포함돼 있지 않아 자동으로 인증 없음.
 */
@Tag(name = "공개 콘텐츠", description = "SEO 유입용 공개 API (로그인 불필요)")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicContentController {

    private final PublicContentService publicContentService;

    @GetMapping("/stats")
    @Operation(summary = "랜딩 페이지 노출용 공개 통계 (회원 수 + 누적 풀이 수)")
    public PublicStatsResponse getStats() {
        return publicContentService.getStats();
    }

    @GetMapping("/ranking")
    @Operation(summary = "랜딩 페이지 노출용 TOP 30 랭킹 (누적 정답 수)")
    public PublicRankingResponse getRanking() {
        return publicContentService.getTopRanking();
    }

    @GetMapping("/certs")
    @Operation(summary = "자격증 목록")
    public List<PublicCertResponse> listCerts() {
        return publicContentService.listCerts();
    }

    @GetMapping("/certs/{certSlug}/categories")
    @Operation(summary = "자격증별 카테고리 목록")
    public List<PublicCategoryResponse> listCategories(@PathVariable String certSlug) {
        return publicContentService.listCategoriesByCert(certSlug);
    }

    @GetMapping("/categories/{categoryId}/questions")
    @Operation(summary = "카테고리별 문제 리스트 (페이지네이션)")
    public PublicQuestionPageResponse listQuestions(
            @PathVariable long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return publicContentService.listQuestionsByCategory(categoryId, page, size);
    }

    @GetMapping("/questions/{id}")
    @Operation(summary = "문제 상세 (정답/해설 포함)")
    public PublicQuestionDetailResponse getQuestion(@PathVariable long id) {
        return publicContentService.getQuestionDetail(id);
    }

    @GetMapping("/questions/ids")
    @Operation(summary = "모든 공개 문제 ID 목록 (sitemap 용)")
    public List<Long> listAllIds() {
        return publicContentService.listAllPublicQuestionIds();
    }
}

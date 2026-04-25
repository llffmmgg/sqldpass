package com.sqldpass.controller.publicapi;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.publicapi.dto.PastExamPublicDtos.PastExamDetail;
import com.sqldpass.controller.publicapi.dto.PastExamPublicDtos.PastExamGradeRequest;
import com.sqldpass.controller.publicapi.dto.PastExamPublicDtos.PastExamGradeResponse;
import com.sqldpass.controller.publicapi.dto.PastExamPublicDtos.PastExamSummary;
import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicCategoryResponse;
import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicCertResponse;
import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicQuestionDetailResponse;
import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicQuestionPageResponse;
import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicSolveQuestionResponse;
import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicSubjectResponse;
import com.sqldpass.controller.publicapi.dto.PublicCertActivityResponse;
import com.sqldpass.controller.publicapi.dto.PublicRankingResponse;
import com.sqldpass.controller.publicapi.dto.PublicStatsResponse;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.publicapi.PastExamPublicService;
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
    private final PastExamPublicService pastExamPublicService;

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

    @GetMapping("/stats/cert-activity")
    @Operation(summary = "자격증별 풀이 활동 (모의고사 / 기출 복원 분리, 누적+오늘자)")
    public PublicCertActivityResponse getCertActivity() {
        return publicContentService.getCertActivity();
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

    @GetMapping("/daily-question")
    @Operation(summary = "자격증별 오늘의 문제 (날짜 시드, 모든 사용자 동일)")
    public PublicQuestionDetailResponse getDailyQuestion(@RequestParam String cert) {
        return publicContentService.getDailyQuestion(cert);
    }

    @PostMapping("/blog/views/{slug}")
    @Operation(summary = "블로그 조회수 증가")
    public void incrementBlogView(@PathVariable String slug) {
        publicContentService.incrementBlogViewCount(slug);
    }

    @GetMapping("/blog/views")
    @Operation(summary = "블로그 전체 조회수 조회")
    public Map<String, Long> getBlogViews() {
        return publicContentService.getAllBlogViewCounts();
    }

    @GetMapping("/subjects")
    @Operation(summary = "Subject 트리 (비로그인 /solve 용)")
    public List<PublicSubjectResponse> listSubjects() {
        return publicContentService.getSubjectTree();
    }

    @GetMapping("/subjects/{id}/random-questions")
    @Operation(summary = "Subject 기반 랜덤 문제 (비로그인 무한 풀이용)")
    public List<PublicSolveQuestionResponse> getRandomQuestions(
            @PathVariable long id,
            @RequestParam(defaultValue = "10") int size) {
        return publicContentService.getRandomSolveQuestions(id, size);
    }

    @PostMapping("/anonymous-solve")
    @Operation(summary = "비회원 풀이 카운터 증가 (집계만)")
    public void incrementAnonymousSolve(@RequestParam(defaultValue = "1") long delta) {
        publicContentService.incrementAnonymousSolve(delta);
    }

    // ================= 기출 복원 (past-exams) — 비로그인 공개 =================

    @GetMapping("/past-exams")
    @Operation(summary = "기출 복원 회차 목록 (자격증별)")
    public List<PastExamSummary> listPastExams(@RequestParam String cert,
                                               jakarta.servlet.http.HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        return pastExamPublicService.listByCert(cert, memberId);
    }

    @GetMapping("/past-exams/{id}")
    @Operation(summary = "기출 복원 회차 상세 (로그인 필수, 정답/해설 미포함)")
    public PastExamDetail getPastExam(@PathVariable Long id,
                                      jakarta.servlet.http.HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        return pastExamPublicService.get(id);
    }

    @PostMapping("/past-exams/{id}/grade")
    @Operation(summary = "기출 복원 회차 채점 (로그인 필수, solve 기록 병행)")
    public PastExamGradeResponse gradePastExam(@PathVariable Long id,
                                               @org.springframework.web.bind.annotation.RequestBody PastExamGradeRequest body,
                                               jakarta.servlet.http.HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        return pastExamPublicService.grade(id, body, memberId);
    }
}

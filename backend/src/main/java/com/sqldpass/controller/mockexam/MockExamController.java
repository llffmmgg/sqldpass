package com.sqldpass.controller.mockexam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.mockexam.dto.MockExamDetailResponse;
import com.sqldpass.controller.mockexam.dto.MockExamSummaryResponse;
import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.payment.MockExamPurchaseRepository;
import com.sqldpass.persistent.solve.SolveRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.mockexam.MockExamService;
import com.sqldpass.service.payment.PaymentProperties;
import com.sqldpass.service.pdf.MockExamPdfService;
import com.sqldpass.service.pdf.MockExamPdfService.DownloadResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Tag(name = "모의고사", description = "모의고사 조회 API (사용자)")
@RestController
@RequestMapping("/api/mock-exams")
@RequiredArgsConstructor
public class MockExamController {

    private final MockExamService mockExamService;
    private final SolveRepository solveRepository;
    private final MockExamPurchaseRepository mockExamPurchaseRepository;
    private final MockExamPdfService mockExamPdfService;
    private final MemberRepository memberRepository;
    private final PaymentProperties paymentProperties;

    @GetMapping
    @Operation(summary = "모의고사 목록", description = "로그인 사용자는 풀이 완료 마킹 + 최고 점수가 함께 응답된다.")
    public List<MockExamSummaryResponse> list(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");

        // 로그인 사용자: 한 번에 mockExamId → (bestCorrect, bestTotal) 맵 조회
        Map<Long, int[]> bestScoreMap = new HashMap<>();
        Set<Long> purchasedIds = new HashSet<>();
        if (memberId != null) {
            for (Object[] row : solveRepository.findBestScoresByMember(memberId)) {
                Long mockExamId = (Long) row[0];
                Integer bestCorrect = ((Number) row[1]).intValue();
                Integer bestTotal = ((Number) row[2]).intValue();
                bestScoreMap.put(mockExamId, new int[]{bestCorrect, bestTotal});
            }
            purchasedIds.addAll(mockExamPurchaseRepository.findMockExamIdsByMemberId(memberId));
        }

        // 사용자 노출은 DRAFT 제외 (PUBLISHED + PREMIUM만 — PREMIUM은 프론트에서 잠금 표시).
        // purchased=true 면 프론트가 결제 페이지로 보내지 않고 바로 풀이 페이지로 진입.
        return mockExamService.getAllForUser().stream()
                .map(exam -> {
                    int[] best = bestScoreMap.get(exam.getId());
                    boolean purchased = purchasedIds.contains(exam.getId());
                    return MockExamSummaryResponse.from(
                            exam,
                            best != null ? best[0] : null,
                            best != null ? best[1] : null,
                            purchased);
                })
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "모의고사 상세 (50문항 포함, 정답 미포함)")
    public MockExamDetailResponse get(@PathVariable Long id, HttpServletRequest request) {
        // PREMIUM이면 403 LOCKED (단, memberId 가 결제 이력이 있으면 통과), DRAFT면 404
        Long memberId = (Long) request.getAttribute("memberId");
        return MockExamDetailResponse.from(mockExamService.getForUser(id, memberId));
    }

    /**
     * 회원의 모의고사·기출 best score 맵.
     * 기출 카탈로그 (/past-exams/{cert}) 가 SSR + ISR 캐시라 회원별 점수를
     * SSR 응답에 못 실음. 클라이언트에서 별도 호출해 카드에 머지하는 용도.
     *
     * 응답: { "{mockExamId}": { "correct": N, "total": M }, ... }
     */
    @GetMapping("/pdf/eligibility")
    @Operation(
            summary = "PDF 다운로드 가능 여부 — 화이트리스트 닉네임만 true (빈 화이트리스트 = 전체 허용)",
            description = "프론트가 모의고사 카드/상세 화면에서 PDF 다운로드 버튼 노출 여부를 결정한다."
    )
    public Map<String, Boolean> pdfEligibility(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        return Map.of("eligible", isPdfEligible(memberId));
    }

    @GetMapping("/{id}/pdf/download")
    @Operation(
            summary = "사용자용 모의고사 PDF 다운로드 (화이트리스트 닉네임 한정)",
            description = "백엔드 프록시 — R2 public URL 노출 없이 PDF 바이트 스트리밍. "
                    + "Content-Disposition: attachment 로 즉시 다운로드 처리. "
                    + "빈 화이트리스트(정식 오픈) 시 모든 로그인 회원 허용."
    )
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id, HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        if (!isPdfEligible(memberId)) {
            throw new SqldpassException(ErrorCode.FORBIDDEN);
        }
        DownloadResult d = mockExamPdfService.download(id);
        String encoded = URLEncoder.encode(d.filename(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(d.bytes());
    }

    /**
     * PDF 다운로드 가시성 — payment.reviewer-nicknames 화이트리스트와 동일 시맨틱.
     *  - 비어있으면 모든 로그인 회원 허용 (정식 오픈 모드).
     *  - 채워있으면 해당 닉네임 회원만 허용.
     */
    private boolean isPdfEligible(Long memberId) {
        if (memberId == null) return false;
        var allowed = paymentProperties.reviewerNicknameSet();
        if (allowed.isEmpty()) return true;
        MemberEntity member = memberRepository.findById(memberId).orElse(null);
        return member != null && allowed.contains(member.getNickname());
    }

    @GetMapping("/best-scores")
    @Operation(summary = "내 모의고사·기출 best score 맵 (mockExamId → {correct, total})")
    public Map<Long, Map<String, Integer>> getMyBestScores(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        Map<Long, Map<String, Integer>> result = new HashMap<>();
        if (memberId == null) return result;
        for (Object[] row : solveRepository.findBestScoresByMember(memberId)) {
            Long mockExamId = (Long) row[0];
            int bestCorrect = ((Number) row[1]).intValue();
            int bestTotal = ((Number) row[2]).intValue();
            result.put(mockExamId, Map.of("correct", bestCorrect, "total", bestTotal));
        }
        return result;
    }
}

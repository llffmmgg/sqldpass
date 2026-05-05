package com.sqldpass.controller.pdf;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.pdf.PrintTokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Playwright 가 인쇄용 페이지 안에서 호출하는 정답·해설 포함 풀데이터 엔드포인트.
 * 단기 토큰(2분) 검증으로만 통과 — 어드민 JWT 와 별도의 purpose-scoped 토큰.
 *
 * 경로 prefix(/api/internal/**) 가 admin/member 인터셉터 화이트리스트 어디에도 들어있지 않아
 * 인터셉터를 우회한다. 인증은 이 컨트롤러의 token 파라미터 검증으로만 한다.
 */
@Tag(name = "내부 - 인쇄", description = "Playwright 인쇄 전용 (단기 토큰)")
@RestController
@RequestMapping("/api/internal/print/mock-exams")
@RequiredArgsConstructor
public class PrintMockExamController {

    private final MockExamRepository mockExamRepository;
    private final PrintTokenService printTokenService;

    @GetMapping("/{id}")
    @Operation(summary = "모의고사 인쇄용 풀데이터 (정답·해설 포함, 단기 토큰 인증)")
    public PrintMockExamResponse get(@PathVariable Long id, @RequestParam("token") String token) {
        if (!printTokenService.validate(token, id)) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED, "유효하지 않거나 만료된 인쇄 토큰입니다.");
        }
        MockExamEntity exam = mockExamRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND));
        return PrintMockExamResponse.from(exam);
    }
}

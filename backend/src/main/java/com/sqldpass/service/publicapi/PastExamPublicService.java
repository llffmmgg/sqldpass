package com.sqldpass.service.publicapi;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.publicapi.dto.PastExamPublicDtos.GradedItem;
import com.sqldpass.controller.publicapi.dto.PastExamPublicDtos.PastExamAnswer;
import com.sqldpass.controller.publicapi.dto.PastExamPublicDtos.PastExamDetail;
import com.sqldpass.controller.publicapi.dto.PastExamPublicDtos.PastExamDetailWithAnswers;
import com.sqldpass.controller.publicapi.dto.PastExamPublicDtos.PastExamGradeRequest;
import com.sqldpass.controller.publicapi.dto.PastExamPublicDtos.PastExamGradeResponse;
import com.sqldpass.controller.publicapi.dto.PastExamPublicDtos.PastExamQuestion;
import com.sqldpass.controller.publicapi.dto.PastExamPublicDtos.PastExamQuestionWithAnswer;
import com.sqldpass.controller.publicapi.dto.PastExamPublicDtos.PastExamSummary;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamKind;
import com.sqldpass.persistent.mockexam.MockExamRepository;
import com.sqldpass.persistent.mockexam.MockExamVisibility;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionMapper;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.solve.SolveRepository;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.controller.solve.dto.SolveAnswerRequest;
import com.sqldpass.controller.solve.dto.SolveRequest;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.grading.SubjectScoringCalculator;
import com.sqldpass.service.solve.GradingService;
import com.sqldpass.service.solve.SolveService;

import java.util.HashMap;

import lombok.RequiredArgsConstructor;

/**
 * 기출 복원 (past-exams) — 비로그인 공개 API 전용 서비스.
 *
 * 조회 규칙:
 * - kind=PAST_EXAM + visibility=PUBLISHED + expert_verified=true 인 회차만 노출
 * - 상세는 정답/해설 미포함 (그래야 DevTools 로 보고 풀기 전에 정답을 알 수 없음)
 * - 채점 엔드포인트에서 정답/해설을 함께 반환
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PastExamPublicService {

    private final MockExamRepository mockExamRepository;
    private final QuestionRepository questionRepository;
    private final GradingService gradingService;
    private final SolveRepository solveRepository;
    private final SolveService solveService;

    public List<PastExamSummary> listByCert(String certSlug, Long memberId) {
        ExamType examType = examTypeFromSlug(certSlug);

        // 로그인 사용자의 경우 모의고사 best score 맵을 1쿼리로 미리 조회 (N+1 방지)
        Map<Long, int[]> bestScoreMap = new HashMap<>();
        if (memberId != null) {
            for (Object[] row : solveRepository.findBestScoresByMember(memberId)) {
                Long mockExamId = (Long) row[0];
                int bestCorrect = ((Number) row[1]).intValue();
                int bestTotal = ((Number) row[2]).intValue();
                bestScoreMap.put(mockExamId, new int[]{bestCorrect, bestTotal});
            }
        }

        return mockExamRepository.findPublicPastExams(examType).stream()
                .map(row -> {
                    MockExamEntity entity = (MockExamEntity) row[0];
                    int count = ((Long) row[1]).intValue();
                    int[] best = bestScoreMap.get(entity.getId());
                    boolean solved = best != null;
                    Integer bestCorrect = best != null ? best[0] : null;
                    Integer bestTotal = best != null ? best[1] : null;
                    return new PastExamSummary(
                            entity.getId(),
                            entity.getName(),
                            entity.getExamType(),
                            certSlug,
                            count,
                            entity.getExamYear(),
                            entity.getExamRound(),
                            entity.getExamDate(),
                            entity.isExpertVerified(),
                            entity.getCreatedAt(),
                            solved,
                            bestCorrect,
                            bestTotal);
                })
                .toList();
    }

    public PastExamDetail get(Long id) {
        MockExamEntity entity = loadPublishedPastExam(id);
        List<PastExamQuestion> questions = entity.getQuestions().stream()
                .map(q -> {
                    SubjectEntity leaf = q.getSubject();
                    SubjectEntity shown = leaf.getParent() != null ? leaf.getParent() : leaf;
                    return new PastExamQuestion(
                            q.getId(),
                            q.getDisplayOrder() != null ? q.getDisplayOrder() : 0,
                            q.getContent(),
                            q.getQuestionType(),
                            shown.getId(),
                            shown.getName());
                })
                .toList();
        return new PastExamDetail(
                entity.getId(),
                entity.getName(),
                entity.getExamType(),
                certSlugFromExamType(entity.getExamType()),
                questions.size(),
                entity.getExamYear(),
                entity.getExamRound(),
                entity.getExamDate(),
                entity.isExpertVerified(),
                questions);
    }

    /**
     * 상세 + 정답·해설. 블로그 SEO 페이지 전용 (검색 노출 목적).
     * 풀이 페이지는 여전히 답이 없는 {@link #get} 를 사용해야 한다.
     */
    public PastExamDetailWithAnswers getWithAnswers(Long id) {
        MockExamEntity entity = loadPublishedPastExam(id);
        List<PastExamQuestionWithAnswer> questions = entity.getQuestions().stream()
                .map(q -> {
                    SubjectEntity leaf = q.getSubject();
                    SubjectEntity shown = leaf.getParent() != null ? leaf.getParent() : leaf;
                    return new PastExamQuestionWithAnswer(
                            q.getId(),
                            q.getDisplayOrder() != null ? q.getDisplayOrder() : 0,
                            q.getContent(),
                            q.getQuestionType(),
                            shown.getId(),
                            shown.getName(),
                            q.getCorrectOption(),
                            q.getAnswer(),
                            QuestionMapper.parseKeywords(q.getKeywords()),
                            q.getExplanation());
                })
                .toList();
        return new PastExamDetailWithAnswers(
                entity.getId(),
                entity.getName(),
                entity.getExamType(),
                certSlugFromExamType(entity.getExamType()),
                questions.size(),
                entity.getExamYear(),
                entity.getExamRound(),
                entity.getExamDate(),
                entity.isExpertVerified(),
                questions);
    }

    /**
     * 채점 — 로그인/비로그인 모두 허용.
     * 로그인 회원이면 solve 테이블에도 적재하여 기출 복원 카드에 최고 점수 스탬프가 표시되도록 한다.
     * 비로그인이면 solveId 는 null 로 반환되며 기록은 저장되지 않는다.
     * 정답/해설은 응답에 포함해 프론트가 결과 화면에서 바로 보여줄 수 있다.
     *
     * 클래스 레벨이 readOnly=true 이므로 solve 저장을 위해 명시적으로 쓰기 트랜잭션으로 열어준다.
     */
    @Transactional
    public PastExamGradeResponse grade(Long id, PastExamGradeRequest request, Long memberId) {
        MockExamEntity entity = loadPublishedPastExam(id);

        Map<Long, QuestionEntity> ownedQuestions = entity.getQuestions().stream()
                .collect(Collectors.toMap(QuestionEntity::getId, q -> q));

        List<PastExamAnswer> answers = request.answers() != null ? request.answers() : List.of();

        List<GradedItem> items = entity.getQuestions().stream()
                .map(q -> {
                    PastExamAnswer submitted = answers.stream()
                            .filter(a -> q.getId().equals(a.questionId()))
                            .findFirst()
                            .orElse(null);
                    Integer selectedOption = submitted != null ? submitted.selectedOption() : null;
                    String answerText = submitted != null ? submitted.answerText() : null;
                    GradingService.GradingResult result = gradingService.grade(q, selectedOption, answerText);
                    return new GradedItem(
                            q.getId(),
                            result.correct(),
                            result.score(),
                            selectedOption,
                            answerText,
                            q.getCorrectOption(),
                            q.getAnswer(),
                            QuestionMapper.parseKeywords(q.getKeywords()),
                            q.getExplanation());
                })
                .toList();

        // 제출 답안 중 기출에 속하지 않는 questionId 는 무시 (안전)
        for (PastExamAnswer a : answers) {
            if (a.questionId() != null && !ownedQuestions.containsKey(a.questionId())) {
                // noop — just skip
            }
        }

        int totalCount = items.size();
        int correctCount = (int) items.stream().filter(GradedItem::correct).count();
        double sum = items.stream().mapToDouble(GradedItem::partialScore).sum();
        int score = totalCount > 0 ? (int) Math.round(sum / totalCount * 100) : 0;

        // 로그인 사용자면 solve 테이블에도 기록 — 최고 점수 스탬프 + history 상세 페이지 진입용
        Long solveId = null;
        if (memberId != null && !items.isEmpty()) {
            try {
                List<SolveAnswerRequest> solveAnswers = items.stream()
                        .map(it -> new SolveAnswerRequest(
                                it.questionId(),
                                it.selectedOption(),
                                it.submittedAnswerText()))
                        .toList();
                var saved = solveService.solve(memberId, new SolveRequest(null, entity.getId(), solveAnswers));
                solveId = saved.solve().getId();
            } catch (Exception ignored) {
                // solve 저장 실패해도 채점 결과는 반환 (공개 API 의 견고성 우선)
            }
        }

        // 자격증별 합격/과락 판정 — 응답의 subjectScores / passed / passReason 채움
        SubjectScoringCalculator.Outcome outcome = SubjectScoringCalculator.compute(
                entity.getExamType(),
                entity.getQuestions(),
                items,
                score);

        return new PastExamGradeResponse(
                totalCount,
                correctCount,
                score,
                items,
                solveId,
                outcome.subjectScores(),
                outcome.passed(),
                outcome.passReason());
    }

    private MockExamEntity loadPublishedPastExam(Long id) {
        MockExamEntity entity = mockExamRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND));
        if (entity.getKind() != MockExamKind.PAST_EXAM
                || entity.getVisibility() != MockExamVisibility.PUBLISHED
                || !entity.isExpertVerified()) {
            // 비로그인 공개 조건 불충족 — 존재 자체를 감춤
            throw new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND);
        }
        return entity;
    }

    public static ExamType examTypeFromSlug(String certSlug) {
        if (certSlug == null) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "cert 파라미터는 필수입니다.");
        }
        return switch (certSlug) {
            case "sqld", "SQLD" -> ExamType.SQLD;
            case "engineer", "ENGINEER_PRACTICAL" -> ExamType.ENGINEER_PRACTICAL;
            case "engineer-written", "ENGINEER_WRITTEN" -> ExamType.ENGINEER_WRITTEN;
            case "computer-literacy-1", "COMPUTER_LITERACY_1" -> ExamType.COMPUTER_LITERACY_1;
            case "computer-literacy-2", "COMPUTER_LITERACY_2" -> ExamType.COMPUTER_LITERACY_2;
            case "adsp", "ADSP" -> ExamType.ADSP;
            default -> throw new SqldpassException(ErrorCode.INVALID_INPUT, "알 수 없는 자격증: " + certSlug);
        };
    }

    public static String certSlugFromExamType(ExamType examType) {
        if (examType == null) return null;
        return switch (examType) {
            case SQLD -> "sqld";
            case ENGINEER_PRACTICAL -> "engineer";
            case ENGINEER_WRITTEN -> "engineer-written";
            case COMPUTER_LITERACY_1 -> "computer-literacy-1";
            case COMPUTER_LITERACY_2 -> "computer-literacy-2";
            case ADSP -> "adsp";
        };
    }
}

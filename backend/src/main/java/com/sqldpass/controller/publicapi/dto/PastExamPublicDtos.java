package com.sqldpass.controller.publicapi.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.question.QuestionType;

/**
 * 기출 복원 (past-exams) 공개 API DTO 모음.
 * - 목록/상세는 정답 비포함
 * - 채점(grade) 응답에만 정답/해설 포함
 */
public final class PastExamPublicDtos {

    private PastExamPublicDtos() {
    }

    /** 목록 카드 */
    public record PastExamSummary(
            Long id,
            String name,
            ExamType examType,
            String certSlug,
            int totalQuestions,
            Integer examYear,
            Integer examRound,
            LocalDate examDate,
            boolean expertVerified,
            LocalDateTime createdAt,
            /** 로그인 사용자가 한 번이라도 풀었는지 (비로그인 false) */
            boolean solved,
            /** 사용자의 최고 정답 수 (미풀이/비로그인 시 null) */
            Integer bestCorrectCount,
            /** 사용자의 최고 풀이 시 총 문항 수 (미풀이/비로그인 시 null) */
            Integer bestTotalCount
    ) {
    }

    /** 상세 — 정답/해설 미포함 */
    public record PastExamDetail(
            Long id,
            String name,
            ExamType examType,
            String certSlug,
            int totalQuestions,
            Integer examYear,
            Integer examRound,
            LocalDate examDate,
            boolean expertVerified,
            List<PastExamQuestion> questions
    ) {
    }

    public record PastExamQuestion(
            Long id,
            int displayOrder,
            String content,
            QuestionType questionType,
            Long subjectId,
            String subjectName
    ) {
    }

    /** 상세 — 블로그 SEO 페이지 전용. 정답·해설 포함. */
    public record PastExamDetailWithAnswers(
            Long id,
            String name,
            ExamType examType,
            String certSlug,
            int totalQuestions,
            Integer examYear,
            Integer examRound,
            LocalDate examDate,
            boolean expertVerified,
            List<PastExamQuestionWithAnswer> questions
    ) {
    }

    public record PastExamQuestionWithAnswer(
            Long id,
            int displayOrder,
            String content,
            QuestionType questionType,
            Long subjectId,
            String subjectName,
            Integer correctOption,
            String answer,
            List<String> keywords,
            String explanation
    ) {
    }

    /** 채점 요청 */
    public record PastExamGradeRequest(
            List<PastExamAnswer> answers
    ) {
    }

    public record PastExamAnswer(
            Long questionId,
            Integer selectedOption,
            String answerText
    ) {
    }

    /**
     * 채점 응답 — 문제별 정답/해설 + 자격증별 합격/과락 판정 포함.
     * solveId 는 history 상세 페이지로 이동하는 용도.
     */
    public record PastExamGradeResponse(
            int totalCount,
            int correctCount,
            int score,
            List<GradedItem> items,
            Long solveId,
            /** 합격 기준 과목 단위 (leaf subject 의 parent) 정답률 / 과락 표시 */
            List<SubjectScore> subjectScores,
            /** 자격증별 공식 합격 기준 적용한 최종 합격 여부 */
            boolean passed,
            /** 합격/불합격 한 줄 요약 (UI 배너) */
            String passReason,
            /** 학습 연속일 마일스톤 도달 일수 — 도달 안 했으면 null, 비로그인이면 null */
            Integer milestoneReached
    ) {
    }

    /** 합격 기준 과목 단위 점수 — 채점 응답에 함께 내려감 */
    public record SubjectScore(
            String subjectName,
            int total,
            int correct,
            /** 정답률 0~100, 소수 한 자리 */
            double rate,
            /** 100점 만점 환산 점수 (자격증별 가중치는 동일하게 정답률 백분율) */
            int weighted,
            /** 자격증의 과목별 과락 컷에 미달했는지 (단일 과목 자격증에선 항상 false) */
            boolean failed
    ) {
    }

    public record GradedItem(
            Long questionId,
            boolean correct,
            double partialScore,
            Integer selectedOption,
            String submittedAnswerText,
            Integer correctOption,
            String answer,
            List<String> keywords,
            String explanation
    ) {
    }
}

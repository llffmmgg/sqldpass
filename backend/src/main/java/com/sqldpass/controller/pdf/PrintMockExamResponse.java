package com.sqldpass.controller.pdf;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamKind;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionMapper;
import com.sqldpass.persistent.question.QuestionType;

/**
 * 인쇄 페이지 (Playwright 가 렌더할 Next.js 페이지) 가 호출하는 정답·해설 포함 풀데이터 응답.
 * 일반 모의고사 조회 API 와 달리 correctOption / answer / keywords / explanation 을 모두 노출한다.
 *
 * 단기 토큰(PrintTokenService) 인증을 통과한 호출자에게만 응답.
 */
public record PrintMockExamResponse(
        Long id,
        String name,
        ExamType examType,
        int sequence,
        int totalQuestions,
        LocalDateTime createdAt,
        boolean expertVerified,
        MockExamKind kind,
        Integer examYear,
        Integer examRound,
        LocalDate examDate,
        List<Question> questions
) {
    public record Question(
            Long id,
            int displayOrder,
            String content,
            QuestionType questionType,
            Integer correctOption,
            String answer,
            List<String> keywords,
            String explanation,
            String topic,
            Integer difficulty,
            String subjectName
    ) {
        public static Question from(QuestionEntity q) {
            return new Question(
                    q.getId(),
                    q.getDisplayOrder() != null ? q.getDisplayOrder() : 0,
                    q.getContent(),
                    q.getQuestionType() != null ? q.getQuestionType() : QuestionType.MCQ,
                    q.getCorrectOption(),
                    q.getAnswer(),
                    QuestionMapper.parseKeywords(q.getKeywords()),
                    q.getExplanation(),
                    q.getTopic(),
                    q.getDifficulty(),
                    q.getSubject() != null ? q.getSubject().getName() : null);
        }
    }

    public static PrintMockExamResponse from(MockExamEntity exam) {
        List<Question> qs = exam.getQuestions().stream()
                .sorted((a, b) -> {
                    Integer ao = a.getDisplayOrder() != null ? a.getDisplayOrder() : Integer.MAX_VALUE;
                    Integer bo = b.getDisplayOrder() != null ? b.getDisplayOrder() : Integer.MAX_VALUE;
                    return ao.compareTo(bo);
                })
                .map(Question::from)
                .toList();
        return new PrintMockExamResponse(
                exam.getId(),
                exam.getName(),
                exam.getExamType(),
                exam.getSequence(),
                qs.size(),
                exam.getCreatedAt(),
                exam.isExpertVerified(),
                exam.getKind(),
                exam.getExamYear(),
                exam.getExamRound(),
                exam.getExamDate(),
                qs);
    }
}

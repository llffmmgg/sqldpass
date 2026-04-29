package com.sqldpass.controller.admin.dto;

import java.time.LocalDate;
import java.util.List;

import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamDifficulty;
import com.sqldpass.persistent.question.QuestionType;

/**
 * 어드민 수동 모의고사 등록 요청 — JSON 한 통으로 모의고사 메타 + 문제 N개를 동시에 적재한다.
 *
 * - examType: 필수
 * - name: 옵셔널. null/blank 면 "{자격증} 모의고사 {sequence}회 ({difficulty라벨})" 자동 생성
 * - difficulty: 옵셔널 (자동 이름 라벨에만 사용). null 이면 NORMAL("보통")
 * - pastExam=true 면 PAST_EXAM 으로 승격 + examYear/examRound/examDate 사용
 * - expertVerified=true 면 등록과 동시에 전문가 검수 완료 처리
 * - questions[*].subjectId 필수, MCQ 면 correctOption 필수, 그 외엔 answer 필수
 */
public record ManualMockExamRequest(
        String name,
        ExamType examType,
        MockExamDifficulty difficulty,
        Boolean pastExam,
        Integer examYear,
        Integer examRound,
        LocalDate examDate,
        Boolean expertVerified,
        List<ManualQuestion> questions
) {
    public record ManualQuestion(
            Long subjectId,
            String content,
            QuestionType questionType,
            Integer correctOption,
            String answer,
            List<String> keywords,
            String explanation,
            String summary,
            String topic,
            Integer difficulty
    ) {}
}

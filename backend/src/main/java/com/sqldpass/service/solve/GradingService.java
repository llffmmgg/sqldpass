package com.sqldpass.service.solve;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionMapper;
import com.sqldpass.persistent.question.QuestionType;

/**
 * 문제 채점 엔진 — MCQ / SHORT_ANSWER / DESCRIPTIVE 분기.
 *
 * 한국산업인력공단 실기 채점 방식을 모방:
 *   - SHORT_ANSWER: 정규화 후 answer 또는 keywords alias 중 매치 → 정답 (pass/fail)
 *   - DESCRIPTIVE: 키워드 포함률 + 답안 길이 조건으로 정답/부분/오답 3단계
 */
@Service
public class GradingService {

    /** 서술형 정답 판정 임계치 */
    private static final double DESCRIPTIVE_PASS_THRESHOLD = 0.7;
    private static final double DESCRIPTIVE_PARTIAL_THRESHOLD = 0.4;
    /** 답안 최소 길이 비율 (모범답안 길이의 X% 이상) — 키워드 나열 부정 방지 */
    private static final double MIN_LENGTH_RATIO = 0.3;

    /**
     * 사용자 답안 채점.
     *
     * @param question 문제 엔티티 (questionType, answer, keywords, correctOption 포함)
     * @param selectedOption MCQ용 1~4 선택지 (비MCQ는 null)
     * @param userAnswerText 단답/서술형 사용자 답안 (MCQ는 null)
     */
    public GradingResult grade(QuestionEntity question, Integer selectedOption, String userAnswerText) {
        QuestionType type = question.getQuestionType() != null ? question.getQuestionType() : QuestionType.MCQ;
        return switch (type) {
            case MCQ -> gradeMcq(question, selectedOption);
            case SHORT_ANSWER -> gradeShortAnswer(question, userAnswerText);
            case DESCRIPTIVE -> gradeDescriptive(question, userAnswerText);
        };
    }

    private GradingResult gradeMcq(QuestionEntity q, Integer selectedOption) {
        Integer correct = q.getCorrectOption();
        boolean isCorrect = correct != null && correct.equals(selectedOption);
        return new GradingResult(isCorrect, isCorrect ? 1.0 : 0.0, List.of());
    }

    private GradingResult gradeShortAnswer(QuestionEntity q, String userAnswer) {
        if (userAnswer == null || userAnswer.isBlank()) {
            return new GradingResult(false, 0.0, List.of());
        }
        String normalizedUser = normalize(userAnswer);

        // 1) 모범답안과 직접 매치
        if (q.getAnswer() != null && normalize(q.getAnswer()).equals(normalizedUser)) {
            return new GradingResult(true, 1.0, List.of(q.getAnswer().trim()));
        }

        // 2) keywords alias 중 매치
        List<String> aliases = QuestionMapper.parseKeywords(q.getKeywords());
        for (String alias : aliases) {
            if (alias != null && normalize(alias).equals(normalizedUser)) {
                return new GradingResult(true, 1.0, List.of(alias));
            }
        }

        // 3) 키워드가 사용자 답안에 포함되기만 해도 (부분 답변 허용)
        //    단, 실기 시험은 엄격하게 완전 매치를 요구하는 경우가 많아 이건 생략.
        return new GradingResult(false, 0.0, List.of());
    }

    private GradingResult gradeDescriptive(QuestionEntity q, String userAnswer) {
        if (userAnswer == null || userAnswer.isBlank()) {
            return new GradingResult(false, 0.0, List.of());
        }
        List<String> keywords = QuestionMapper.parseKeywords(q.getKeywords());
        if (keywords.isEmpty()) {
            // 채점 키워드 없으면 채점 불가 — 오답 처리 (운영 데이터 품질 문제)
            return new GradingResult(false, 0.0, List.of());
        }

        String normalizedUser = normalize(userAnswer);
        List<String> matched = new ArrayList<>();
        for (String kw : keywords) {
            if (kw == null || kw.isBlank()) continue;
            if (normalizedUser.contains(normalize(kw))) {
                matched.add(kw);
            }
        }
        double ratio = (double) matched.size() / keywords.size();

        // 길이 조건 — 모범답안 길이의 30% 미만이면 자동 오답 (키워드 나열 부정 방지)
        int userLen = userAnswer.trim().length();
        int answerLen = q.getAnswer() != null ? q.getAnswer().trim().length() : 0;
        boolean lengthOk = answerLen == 0 || userLen >= answerLen * MIN_LENGTH_RATIO;

        if (!lengthOk) {
            return new GradingResult(false, 0.0, matched);
        }

        if (ratio >= DESCRIPTIVE_PASS_THRESHOLD) {
            return new GradingResult(true, 1.0, matched);  // 정답 (100%)
        } else if (ratio >= DESCRIPTIVE_PARTIAL_THRESHOLD) {
            return new GradingResult(false, 0.5, matched);  // 부분점수 (50%) — isCorrect=false지만 점수 있음
        } else {
            return new GradingResult(false, 0.0, matched);  // 오답
        }
    }

    /** 공백/대소문자/양끝 특수문자 정규화 */
    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim()
                .toLowerCase()
                .replaceAll("\\s+", " ")
                .replaceAll("[\\.\\,\\!\\?;:]+$", "");
    }

    /**
     * 채점 결과.
     * @param correct 정답 여부 (부분점수면 false)
     * @param score 0.0 ~ 1.0 (MCQ/SHORT는 0 또는 1, DESCRIPTIVE는 0 / 0.5 / 1.0)
     * @param matchedKeywords 채점 시 매치된 키워드 (사용자에게 피드백용)
     */
    public record GradingResult(boolean correct, double score, List<String> matchedKeywords) {
    }
}

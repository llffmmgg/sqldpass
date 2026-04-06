package com.sqldpass.service.solve;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionType;

class GradingServiceTest {

    private final GradingService gradingService = new GradingService();

    // 리플렉션으로 QuestionEntity 생성 (protected 생성자 우회)
    private QuestionEntity newQuestion(QuestionType type, Integer correctOption, String answer, String keywordsJson) {
        try {
            java.lang.reflect.Constructor<QuestionEntity> ctor = QuestionEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            QuestionEntity q = ctor.newInstance();
            setField(q, "questionType", type);
            setField(q, "correctOption", correctOption);
            setField(q, "answer", answer);
            setField(q, "keywords", keywordsJson);
            setField(q, "content", "dummy content");
            return q;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String name, Object value) {
        try {
            Field f = QuestionEntity.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("MCQ - 선택지가 정답과 일치하면 정답")
    void mcq_correct() {
        QuestionEntity q = newQuestion(QuestionType.MCQ, 2, null, null);
        GradingService.GradingResult r = gradingService.grade(q, 2, null);
        assertThat(r.correct()).isTrue();
        assertThat(r.score()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("MCQ - 선택지 불일치면 오답")
    void mcq_wrong() {
        QuestionEntity q = newQuestion(QuestionType.MCQ, 2, null, null);
        GradingService.GradingResult r = gradingService.grade(q, 3, null);
        assertThat(r.correct()).isFalse();
        assertThat(r.score()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("MCQ - 미답(null)은 오답")
    void mcq_null() {
        QuestionEntity q = newQuestion(QuestionType.MCQ, 2, null, null);
        GradingService.GradingResult r = gradingService.grade(q, null, null);
        assertThat(r.correct()).isFalse();
    }

    @Test
    @DisplayName("SHORT_ANSWER - 정규화 후 모범답안 일치")
    void shortAnswer_exact() {
        QuestionEntity q = newQuestion(QuestionType.SHORT_ANSWER, null, "15", "[\"15\"]");
        assertThat(gradingService.grade(q, null, "15").correct()).isTrue();
        assertThat(gradingService.grade(q, null, " 15 ").correct()).isTrue();
        assertThat(gradingService.grade(q, null, "16").correct()).isFalse();
    }

    @Test
    @DisplayName("SHORT_ANSWER - keywords alias 중 하나만 맞아도 정답")
    void shortAnswer_alias() {
        QuestionEntity q = newQuestion(QuestionType.SHORT_ANSWER, null, "전송 계층",
                "[\"전송 계층\", \"Transport Layer\", \"4계층\"]");
        assertThat(gradingService.grade(q, null, "Transport Layer").correct()).isTrue();
        assertThat(gradingService.grade(q, null, "transport layer").correct()).isTrue();  // 대소문자 무시
        assertThat(gradingService.grade(q, null, "4계층").correct()).isTrue();
        assertThat(gradingService.grade(q, null, "응용 계층").correct()).isFalse();
    }

    @Test
    @DisplayName("SHORT_ANSWER - 빈 답변은 오답")
    void shortAnswer_empty() {
        QuestionEntity q = newQuestion(QuestionType.SHORT_ANSWER, null, "15", "[\"15\"]");
        assertThat(gradingService.grade(q, null, "").correct()).isFalse();
        assertThat(gradingService.grade(q, null, "   ").correct()).isFalse();
        assertThat(gradingService.grade(q, null, null).correct()).isFalse();
    }

    @Test
    @DisplayName("DESCRIPTIVE - 키워드 70% 이상 + 길이 만족 → 정답")
    void descriptive_pass() {
        String answer = "결합도는 낮게, 응집도는 높게 하는 것이 좋은 모듈 설계입니다. 이를 통해 유지보수성이 향상됩니다.";
        String keywords = "[\"결합도\", \"응집도\", \"낮은 결합도\", \"높은 응집도\", \"유지보수성\"]";
        QuestionEntity q = newQuestion(QuestionType.DESCRIPTIVE, null, answer, keywords);

        // 5개 키워드 중 5개 포함 (100%), 충분한 길이
        String userAnswer = "결합도는 모듈 간 의존도이며 낮은 결합도가 바람직하고, 응집도는 높을수록(높은 응집도) 좋다. 이는 유지보수성을 향상시킨다.";
        GradingService.GradingResult r = gradingService.grade(q, null, userAnswer);
        assertThat(r.correct()).isTrue();
        assertThat(r.score()).isEqualTo(1.0);
        assertThat(r.matchedKeywords()).hasSize(5);
    }

    @Test
    @DisplayName("DESCRIPTIVE - 키워드 40~70% + 길이 만족 → 부분점수")
    void descriptive_partial() {
        String answer = "결합도는 낮게, 응집도는 높게 하는 것이 좋은 모듈 설계입니다. 이를 통해 유지보수성이 향상됩니다.";
        String keywords = "[\"결합도\", \"응집도\", \"낮은 결합도\", \"높은 응집도\", \"유지보수성\"]";
        QuestionEntity q = newQuestion(QuestionType.DESCRIPTIVE, null, answer, keywords);

        // 5개 중 2개(결합도, 응집도) = 40%
        String userAnswer = "결합도와 응집도는 모듈 설계에서 중요한 개념입니다. 둘의 균형이 중요합니다.";
        GradingService.GradingResult r = gradingService.grade(q, null, userAnswer);
        assertThat(r.correct()).isFalse();
        assertThat(r.score()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("DESCRIPTIVE - 키워드 나열 부정 방지 (너무 짧으면 오답)")
    void descriptive_tooShort() {
        // 긴 모범답안 (약 200자) → 30% = 60자. 사용자는 키워드만 나열한 30자 → 길이 부족으로 오답.
        String answer = "결합도는 모듈 간의 상호 의존 정도를 나타내며 낮을수록 좋다. 응집도는 모듈 내부 요소들이 하나의 기능을 위해 얼마나 밀접하게 관련되어 있는지를 나타내며 높을수록 좋다. 좋은 모듈 설계란 낮은 결합도와 높은 응집도를 가지는 것으로, 모듈 독립성을 높여 유지보수성을 향상시킨다.";
        String keywords = "[\"결합도\", \"응집도\", \"낮은 결합도\", \"높은 응집도\", \"유지보수성\"]";
        QuestionEntity q = newQuestion(QuestionType.DESCRIPTIVE, null, answer, keywords);

        // 모든 키워드 나열만 (30자, 길이 30% 미달)
        String userAnswer = "결합도 응집도 낮은 결합도 높은 응집도 유지보수성";
        GradingService.GradingResult r = gradingService.grade(q, null, userAnswer);
        assertThat(r.correct()).isFalse();
        assertThat(r.score()).isEqualTo(0.0);
    }
}

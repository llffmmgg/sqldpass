package com.sqldpass.persistent.solve;

import com.sqldpass.persistent.common.BaseTimeEntity;
import com.sqldpass.persistent.question.QuestionEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "solve_answer", indexes = {
    @Index(name = "idx_solve_answer_solve_id", columnList = "solve_id"),
    @Index(name = "idx_solve_answer_question_id", columnList = "question_id")
})
public class SolveAnswerEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solve_id", nullable = false)
    private SolveEntity solve;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    /** MCQ 선택지 1~4. 비-MCQ는 NULL */
    @Column(name = "selected_option")
    private Integer selectedOption;

    /** MCQ 정답 스냅샷 (제출 시점). 비-MCQ는 NULL */
    @Column(name = "correct_option")
    private Integer correctOption;

    /** 단답/서술형 사용자 답안 */
    @Column(name = "user_answer_text", columnDefinition = "TEXT")
    private String userAnswerText;

    /** 채점 시 매치된 키워드 (JSON 배열 문자열) */
    @Column(name = "matched_keywords", columnDefinition = "TEXT")
    private String matchedKeywords;

    /** 0.000 ~ 1.000 부분점수 */
    @Column(name = "partial_score", precision = 4, scale = 3)
    private java.math.BigDecimal partialScore;

    @Column(nullable = false)
    private boolean isCorrect;

    /** MCQ 전용 생성자 (기존 SQLD 경로 유지) */
    public SolveAnswerEntity(SolveEntity solve, QuestionEntity question, int selectedOption, int correctOption, boolean isCorrect) {
        this.solve = solve;
        this.question = question;
        this.selectedOption = selectedOption;
        this.correctOption = correctOption;
        this.isCorrect = isCorrect;
        this.partialScore = java.math.BigDecimal.valueOf(isCorrect ? 1.0 : 0.0);
    }

    /** 단답/서술 전용 생성자 */
    public SolveAnswerEntity(SolveEntity solve, QuestionEntity question, String userAnswerText,
                             String matchedKeywords, double score, boolean isCorrect) {
        this.solve = solve;
        this.question = question;
        this.userAnswerText = userAnswerText;
        this.matchedKeywords = matchedKeywords;
        this.partialScore = java.math.BigDecimal.valueOf(score);
        this.isCorrect = isCorrect;
    }
}

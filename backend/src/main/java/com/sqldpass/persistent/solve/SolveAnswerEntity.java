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

    @Column(name = "selected_option", nullable = false, columnDefinition = "TINYINT")
    private int selectedOption;

    @Column(name = "correct_option", nullable = false, columnDefinition = "TINYINT")
    private int correctOption;

    @Column(nullable = false)
    private boolean isCorrect;

    public SolveAnswerEntity(SolveEntity solve, QuestionEntity question, int selectedOption, int correctOption, boolean isCorrect) {
        this.solve = solve;
        this.question = question;
        this.selectedOption = selectedOption;
        this.correctOption = correctOption;
        this.isCorrect = isCorrect;
    }
}

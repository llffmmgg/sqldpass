package com.sqldpass.persistent.question;

import com.sqldpass.persistent.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "question_option", uniqueConstraints = {
    @UniqueConstraint(name = "uk_question_option_number", columnNames = {"question_id", "option_number"})
})
public class QuestionOptionEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    @Column(name = "option_number", nullable = false)
    private int optionNumber;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    private boolean isCorrect;

    @Builder
    public QuestionOptionEntity(QuestionEntity question, int optionNumber, String content, boolean isCorrect) {
        this.question = question;
        this.optionNumber = optionNumber;
        this.content = content;
        this.isCorrect = isCorrect;
    }
}

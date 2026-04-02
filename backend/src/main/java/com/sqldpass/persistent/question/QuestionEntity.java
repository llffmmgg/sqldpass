package com.sqldpass.persistent.question;

import com.sqldpass.persistent.common.BaseTimeEntity;
import com.sqldpass.persistent.subject.SubjectEntity;

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
@Table(name = "question", indexes = {
    @Index(name = "idx_question_subject_id", columnList = "subject_id")
})
public class QuestionEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private SubjectEntity subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "correct_option", nullable = false, columnDefinition = "TINYINT")
    private int correctOption;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(length = 200)
    private String summary;

    public QuestionEntity(SubjectEntity subject, String content, int correctOption, String explanation) {
        this.subject = subject;
        this.content = content;
        this.correctOption = correctOption;
        this.explanation = explanation;
    }

    public QuestionEntity(SubjectEntity subject, String content, int correctOption, String explanation, String summary) {
        this.subject = subject;
        this.content = content;
        this.correctOption = correctOption;
        this.explanation = explanation;
        this.summary = summary;
    }

    public void update(String content, int correctOption, String explanation, String summary) {
        this.content = content;
        this.correctOption = correctOption;
        this.explanation = explanation;
        this.summary = summary;
    }
}

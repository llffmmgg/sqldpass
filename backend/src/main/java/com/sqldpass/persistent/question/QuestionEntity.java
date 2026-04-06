package com.sqldpass.persistent.question;

import com.sqldpass.persistent.common.BaseTimeEntity;
import com.sqldpass.persistent.mockexam.MockExamEntity;
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
    @Index(name = "idx_question_subject_id", columnList = "subject_id"),
    @Index(name = "idx_question_subject_mock", columnList = "subject_id, mock_exam_id")
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

    @Column(length = 50)
    private String topic;

    @Column(columnDefinition = "TINYINT")
    private Integer difficulty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mock_exam_id")
    private MockExamEntity mockExam;

    @Column(name = "display_order")
    private Integer displayOrder;

    public QuestionEntity(SubjectEntity subject, String content, int correctOption, String explanation) {
        this.subject = subject;
        this.content = content;
        this.correctOption = correctOption;
        this.explanation = explanation;
    }

    public QuestionEntity(SubjectEntity subject, String content, int correctOption, String explanation,
                          String summary, String topic, Integer difficulty) {
        this.subject = subject;
        this.content = content;
        this.correctOption = correctOption;
        this.explanation = explanation;
        this.summary = summary;
        this.topic = topic;
        this.difficulty = difficulty;
    }

    public void update(String content, int correctOption, String explanation, String summary) {
        this.content = content;
        this.correctOption = correctOption;
        this.explanation = explanation;
        this.summary = summary;
    }

    public void assignToMockExam(MockExamEntity mockExam, int displayOrder) {
        this.mockExam = mockExam;
        this.displayOrder = displayOrder;
    }

    public void releaseFromMockExam() {
        this.mockExam = null;
        this.displayOrder = null;
    }
}

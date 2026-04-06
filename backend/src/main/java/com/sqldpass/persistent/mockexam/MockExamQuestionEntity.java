package com.sqldpass.persistent.mockexam;

import com.sqldpass.persistent.common.BaseTimeEntity;
import com.sqldpass.persistent.question.QuestionEntity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "mock_exam_question", uniqueConstraints = {
    @UniqueConstraint(name = "uk_meq_question", columnNames = {"mock_exam_id", "question_id"}),
    @UniqueConstraint(name = "uk_meq_order", columnNames = {"mock_exam_id", "display_order"})
})
public class MockExamQuestionEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mock_exam_id", nullable = false)
    private MockExamEntity mockExam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public MockExamQuestionEntity(MockExamEntity mockExam, QuestionEntity question, int displayOrder) {
        this.mockExam = mockExam;
        this.question = question;
        this.displayOrder = displayOrder;
    }
}

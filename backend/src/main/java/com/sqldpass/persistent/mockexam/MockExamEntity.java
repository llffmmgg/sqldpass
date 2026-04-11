package com.sqldpass.persistent.mockexam;

import java.util.ArrayList;
import java.util.List;

import com.sqldpass.persistent.common.BaseTimeEntity;
import com.sqldpass.persistent.question.QuestionEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "mock_exam", uniqueConstraints = {
    @UniqueConstraint(name = "uk_mock_exam_exam_type_sequence", columnNames = {"exam_type", "sequence"})
})
public class MockExamEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false, length = 30)
    private ExamType examType = ExamType.SQLD;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "template", length = 32)
    private EngineerExamTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private MockExamVisibility visibility = MockExamVisibility.DRAFT;

    @Column(name = "expert_verified", nullable = false)
    private boolean expertVerified = false;

    @OneToMany(mappedBy = "mockExam")
    @OrderBy("displayOrder ASC")
    private List<QuestionEntity> questions = new ArrayList<>();

    public MockExamEntity(String name, int sequence) {
        this(name, ExamType.SQLD, sequence, null);
    }

    public MockExamEntity(String name, ExamType examType, int sequence) {
        this(name, examType, sequence, null);
    }

    public MockExamEntity(String name, ExamType examType, int sequence, EngineerExamTemplate template) {
        this.name = name;
        this.examType = examType;
        this.sequence = sequence;
        this.template = template;
        this.visibility = MockExamVisibility.DRAFT;
    }

    /** 양방향 동기화 — 문제에 모의고사 배정 + 컬렉션에도 추가 */
    public void linkQuestion(QuestionEntity question, int displayOrder) {
        question.assignToMockExam(this, displayOrder);
        this.questions.add(question);
    }

    public void changeVisibility(MockExamVisibility visibility) {
        if (visibility == null) {
            throw new IllegalArgumentException("visibility는 null일 수 없습니다.");
        }
        this.visibility = visibility;
    }

    public void toggleExpertVerified() {
        this.expertVerified = !this.expertVerified;
    }
}

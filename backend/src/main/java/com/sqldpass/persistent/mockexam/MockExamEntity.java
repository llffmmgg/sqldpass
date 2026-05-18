package com.sqldpass.persistent.mockexam;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private MockExamKind kind = MockExamKind.AI;

    @Column(name = "exam_year")
    private Integer examYear;

    @Column(name = "exam_round")
    private Integer examRound;

    @Column(name = "exam_date")
    private LocalDate examDate;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "past_exam_linked_at")
    private LocalDateTime pastExamLinkedAt;

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
        this.kind = MockExamKind.AI;
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
        // 최초 공개 시점만 기록 — 한 번 공개한 회차는 PUBLISHED↔PREMIUM 사이를 오가도
        // publishedAt 을 보존해 NEW 뱃지 트리거 시점이 흔들리지 않게 한다.
        if (this.publishedAt == null && visibility != MockExamVisibility.DRAFT) {
            this.publishedAt = LocalDateTime.now();
        }
        this.visibility = visibility;
    }

    public void toggleExpertVerified() {
        this.expertVerified = !this.expertVerified;
    }

    /**
     * 기출 복원 메타로 승격 — kind=PAST_EXAM 으로 고정하고 연도/회차/시험일 세팅.
     * kind 를 AI 로 되돌리고 싶으면 {@link #demoteToAi()} 호출.
     */
    public void promoteToPastExam(Integer examYear, Integer examRound, LocalDate examDate) {
        this.kind = MockExamKind.PAST_EXAM;
        this.examYear = examYear;
        this.examRound = examRound;
        this.examDate = examDate;
        this.pastExamLinkedAt = LocalDateTime.now();
    }

    public void demoteToAi() {
        this.kind = MockExamKind.AI;
        this.examYear = null;
        this.examRound = null;
        this.examDate = null;
        this.pastExamLinkedAt = null;
    }

    /** 미니 모의고사로 마킹 — 어드민 일괄 생성 시 호출. */
    public void markAsMini() {
        this.kind = MockExamKind.MINI;
    }
}

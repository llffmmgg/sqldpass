package com.sqldpass.persistent.solve;

import java.util.ArrayList;
import java.util.List;

import com.sqldpass.persistent.common.BaseTimeEntity;
import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.subject.SubjectEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "solve", indexes = {
    @Index(name = "idx_solve_member_id", columnList = "member_id"),
    @Index(name = "idx_solve_client_submission", columnList = "member_id, client_submission_id")
})
public class SolveEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private SubjectEntity subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mock_exam_id")
    private MockExamEntity mockExam;

    @Column(nullable = false)
    private int totalCount;

    @Column(nullable = false)
    private int correctCount;

    @Column(nullable = false)
    private int score;

    @Column(name = "client_submission_id", length = 64)
    private String clientSubmissionId;

    @OneToMany(mappedBy = "solve", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SolveAnswerEntity> answers = new ArrayList<>();

    public SolveEntity(MemberEntity member, SubjectEntity subject, int totalCount, int correctCount, int score) {
        this(member, subject, totalCount, correctCount, score, null);
    }

    public SolveEntity(
            MemberEntity member,
            SubjectEntity subject,
            int totalCount,
            int correctCount,
            int score,
            String clientSubmissionId) {
        this.member = member;
        this.subject = subject;
        this.totalCount = totalCount;
        this.correctCount = correctCount;
        this.score = score;
        this.clientSubmissionId = normalizeClientSubmissionId(clientSubmissionId);
    }

    public SolveEntity(MemberEntity member, MockExamEntity mockExam, int totalCount, int correctCount, int score) {
        this(member, mockExam, totalCount, correctCount, score, null);
    }

    public SolveEntity(
            MemberEntity member,
            MockExamEntity mockExam,
            int totalCount,
            int correctCount,
            int score,
            String clientSubmissionId) {
        this.member = member;
        this.mockExam = mockExam;
        this.totalCount = totalCount;
        this.correctCount = correctCount;
        this.score = score;
        this.clientSubmissionId = normalizeClientSubmissionId(clientSubmissionId);
    }

    public SolveEntity(MemberEntity member, int totalCount, int correctCount, int score) {
        this(member, totalCount, correctCount, score, null);
    }

    public SolveEntity(
            MemberEntity member,
            int totalCount,
            int correctCount,
            int score,
            String clientSubmissionId) {
        this.member = member;
        this.totalCount = totalCount;
        this.correctCount = correctCount;
        this.score = score;
        this.clientSubmissionId = normalizeClientSubmissionId(clientSubmissionId);
    }

    public void addAnswer(SolveAnswerEntity answer) {
        this.answers.add(answer);
    }

    private static String normalizeClientSubmissionId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

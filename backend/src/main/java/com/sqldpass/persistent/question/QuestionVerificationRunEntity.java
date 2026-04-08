package com.sqldpass.persistent.question;

import java.time.LocalDateTime;

import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.subject.SubjectEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "question_verification_run")
public class QuestionVerificationRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", length = 30)
    private ExamType examType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private SubjectEntity subject;

    @Column(name = "subject_name", length = 100)
    private String subjectName;

    @Column(name = "limit_requested", nullable = false)
    private int limitRequested;

    @Column(name = "force_recheck", nullable = false)
    private boolean forceRecheck;

    @Column(name = "processed_count", nullable = false)
    private int processedCount;

    @Column(name = "suspicious_count", nullable = false)
    private int suspiciousCount;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    public QuestionVerificationRunEntity(ExamType examType, SubjectEntity subject, String subjectName,
                                         int limitRequested, boolean forceRecheck,
                                         int processedCount, int suspiciousCount,
                                         LocalDateTime completedAt) {
        this.examType = examType;
        this.subject = subject;
        this.subjectName = subjectName;
        this.limitRequested = limitRequested;
        this.forceRecheck = forceRecheck;
        this.processedCount = processedCount;
        this.suspiciousCount = suspiciousCount;
        this.completedAt = completedAt;
    }
}

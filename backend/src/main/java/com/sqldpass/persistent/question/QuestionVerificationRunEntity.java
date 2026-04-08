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

    /** 자동 fix가 성공해 DB에 반영된 문제 수 */
    @Column(name = "fixed_count", nullable = false)
    private int fixedCount;

    /** 거절됐지만 자동 fix가 실패해 수동 검토가 필요한 문제 수 */
    @Column(name = "unfixable_count", nullable = false)
    private int unfixableCount;

    /** UNKNOWN(빈 응답/파싱 실패)으로 다음 회차에 재시도되는 문제 수 */
    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    public QuestionVerificationRunEntity(ExamType examType, SubjectEntity subject, String subjectName,
                                         int limitRequested, boolean forceRecheck,
                                         int processedCount, int suspiciousCount,
                                         int fixedCount, int unfixableCount, int errorCount,
                                         LocalDateTime completedAt) {
        this.examType = examType;
        this.subject = subject;
        this.subjectName = subjectName;
        this.limitRequested = limitRequested;
        this.forceRecheck = forceRecheck;
        this.processedCount = processedCount;
        this.suspiciousCount = suspiciousCount;
        this.fixedCount = fixedCount;
        this.unfixableCount = unfixableCount;
        this.errorCount = errorCount;
        this.completedAt = completedAt;
    }
}

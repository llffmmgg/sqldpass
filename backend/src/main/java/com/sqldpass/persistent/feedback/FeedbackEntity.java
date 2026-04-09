package com.sqldpass.persistent.feedback;

import java.time.LocalDateTime;

import com.sqldpass.persistent.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "feedback", indexes = {
        @Index(name = "idx_feedback_status", columnList = "status"),
        @Index(name = "idx_feedback_question_id", columnList = "question_id"),
        @Index(name = "idx_feedback_member_id", columnList = "member_id"),
        @Index(name = "idx_feedback_created_at", columnList = "created_at")
})
public class FeedbackEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FeedbackType type;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "question_id")
    private Long questionId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "page_url", length = 500)
    private String pageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackStatus status = FeedbackStatus.NEW;

    @Column(name = "admin_reply", columnDefinition = "TEXT")
    private String adminReply;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    public FeedbackEntity(FeedbackType type, Long memberId, Long questionId,
                          String content, String pageUrl) {
        this.type = type;
        this.memberId = memberId;
        this.questionId = questionId;
        this.content = content;
        this.pageUrl = pageUrl;
        this.status = FeedbackStatus.NEW;
    }

    public void changeStatus(FeedbackStatus status) {
        this.status = status;
    }

    /**
     * 어드민 답변 작성. 답변 본문과 시각을 저장하고 status 를 RESOLVED 로 전이한다.
     * 알림 발송 여부는 호출 측(Service)이 prev status 를 보고 판단한다.
     */
    public void writeReply(String reply) {
        this.adminReply = reply;
        this.repliedAt = LocalDateTime.now();
        this.status = FeedbackStatus.RESOLVED;
    }
}

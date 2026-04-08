package com.sqldpass.persistent.question;

import java.time.LocalDateTime;

import com.sqldpass.persistent.common.BaseTimeEntity;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.subject.SubjectEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType questionType = QuestionType.MCQ;

    /** MCQ일 때만 의미 있음. 비MCQ는 NULL */
    @Column(name = "correct_option", columnDefinition = "TINYINT")
    private Integer correctOption;

    /** SHORT_ANSWER/DESCRIPTIVE 정답(모범답안) */
    @Column(columnDefinition = "TEXT")
    private String answer;

    /** JSON 배열 문자열. SHORT_ANSWER: 허용 alias 리스트. DESCRIPTIVE: 채점 키워드 리스트 */
    @Column(columnDefinition = "TEXT")
    private String keywords;

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

    /** 어드민 LLM 검증용 export 마크. NULL이면 한 번도 export 안 됨. */
    @Column(name = "exported_at")
    private LocalDateTime exportedAt;

    /** 어드민 직접 LLM 검증 완료 시각. 수정되면 다시 NULL로 돌아감. */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /** 본문 normalize → SHA-256 hex. 모의고사 회차 간/내 중복 검증용. NULL 허용 (legacy 호환) */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

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

    /** 정처기 단답/약술형 생성용 — correctOption 미사용, answer/keywords 사용 */
    public QuestionEntity(SubjectEntity subject, String content, QuestionType questionType,
                          String answer, String keywords, String explanation,
                          String summary, String topic, Integer difficulty) {
        this.subject = subject;
        this.content = content;
        this.questionType = questionType;
        this.answer = answer;
        this.keywords = keywords;
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
        this.verifiedAt = null;
    }

    /** 어드민 MCQ 수정 — answer/keywords는 건드리지 않음 */
    public void updateMcq(String content, int correctOption, String explanation, String summary) {
        this.content = content;
        this.questionType = QuestionType.MCQ;
        this.correctOption = correctOption;
        this.explanation = explanation;
        this.summary = summary;
        this.answer = null;
        this.keywords = null;
        this.verifiedAt = null;
    }

    /** 어드민 단답/약술형 수정 — correct_option은 NULL로 강제, answer/keywords 갱신 */
    public void updateShortAnswer(String content, QuestionType questionType, String answer,
                                  String keywordsJson, String explanation, String summary) {
        this.content = content;
        this.questionType = questionType;
        this.correctOption = null;
        this.answer = answer;
        this.keywords = keywordsJson;
        this.explanation = explanation;
        this.summary = summary;
        this.verifiedAt = null;
    }

    public void markVerified(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public void assignToMockExam(MockExamEntity mockExam, int displayOrder) {
        this.mockExam = mockExam;
        this.displayOrder = displayOrder;
    }

    public void assignContentHash(String hash) {
        this.contentHash = hash;
    }

    public void releaseFromMockExam() {
        this.mockExam = null;
        this.displayOrder = null;
    }
}

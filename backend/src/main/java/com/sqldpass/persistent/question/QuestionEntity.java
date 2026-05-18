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
    @Index(name = "idx_question_subject_mock", columnList = "subject_id, mock_exam_id"),
    @Index(name = "idx_question_verification_category", columnList = "verification_category")
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

    /** 마지막 검증 결과 카테고리. 어드민이 카테고리별 목록을 조회하기 위해 영속화. 수정 시 NONE 으로 리셋. */
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_category", nullable = false, length = 20)
    private VerificationCategory verificationCategory = VerificationCategory.NONE;

    /** 본문 normalize → SHA-256 hex. 모의고사 회차 간/내 중복 검증용. NULL 허용 (legacy 호환) */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    /**
     * 미니 모의고사로 복제된 시각. NULL 이면 다음 미니 풀 후보, NOT NULL 이면 이미 한 번 복제됨.
     * 원본 문제에만 세팅되며, 복제본은 항상 NULL 로 시작한다.
     */
    @Column(name = "included_in_mini_at")
    private LocalDateTime includedInMiniAt;

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
        this.verificationCategory = VerificationCategory.NONE;
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
        this.verificationCategory = VerificationCategory.NONE;
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
        this.verificationCategory = VerificationCategory.NONE;
    }

    public void markVerified(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
        this.verificationCategory = VerificationCategory.NONE;
    }

    public void setVerificationCategory(VerificationCategory category) {
        this.verificationCategory = category;
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

    /**
     * 미니 모의고사 복제 생성자 — 원본의 내용/정답/검증 메타를 복사한다.
     * 다음은 의도적으로 비운다:
     *  - id (DB 가 새로 부여)
     *  - mock_exam, display_order (linkQuestion 으로 새 미니 회차에 link)
     *  - content_hash (AI 생성 중복 체크와 충돌 방지 — 원본 해시는 그대로 풀에 존재)
     *  - exported_at, included_in_mini_at (복제본은 원점 상태)
     */
    public QuestionEntity(QuestionEntity origin) {
        this.subject = origin.subject;
        this.content = origin.content;
        this.questionType = origin.questionType;
        this.correctOption = origin.correctOption;
        this.answer = origin.answer;
        this.keywords = origin.keywords;
        this.explanation = origin.explanation;
        this.summary = origin.summary;
        this.topic = origin.topic;
        this.difficulty = origin.difficulty;
        this.verifiedAt = origin.verifiedAt;
        this.verificationCategory = origin.verificationCategory;
    }

    /** 미니 모의고사 풀에서 복제 사용됐음을 기록 — 다음 미니 생성에서 제외된다. */
    public void markIncludedInMini(LocalDateTime now) {
        this.includedInMiniAt = now;
    }
}

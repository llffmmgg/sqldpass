package com.sqldpass.persistent.question;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {

    /**
     * 카테고리(과목) 풀에서 사용 가능한 question id 조회.
     *
     * - SQLD: mockExam IS NULL 인 미편성 문제만 (모의고사로 편성되면 풀에서 빠짐)
     * - 정처기 실기: 모의고사 편성 여부와 무관하게 모두 노출 (모의고사 생성이 곧 풀 적재)
     *
     * 자격증 구분은 question 자체에 컬럼이 없으므로 mockExam.examType 으로 판별한다.
     * 정처기 문제는 항상 mockExam이 세팅된 상태로 생성되므로 examType=ENGINEER_PRACTICAL.
     */
    @Query("SELECT q.id FROM QuestionEntity q LEFT JOIN q.mockExam m " +
            "WHERE q.subject.id = :subjectId " +
            "  AND (m IS NULL OR m.examType = com.sqldpass.persistent.mockexam.ExamType.ENGINEER_PRACTICAL)")
    List<Long> findAvailableIdsBySubjectId(@Param("subjectId") Long subjectId);

    /**
     * 과목별 랜덤 문항 추출 (모의고사 미편성 분).
     * ORDER BY RAND() 대신 후보 ID를 가져와 JVM에서 셔플 → 필요한 만큼 IN-list로 조회.
     * 풀이 커져도 full table scan + filesort 유발하지 않음.
     */
    default List<QuestionEntity> findRandomBySubjectId(Long subjectId, int limit) {
        List<Long> candidateIds = new ArrayList<>(findAvailableIdsBySubjectId(subjectId));
        if (candidateIds.isEmpty()) {
            return List.of();
        }
        Collections.shuffle(candidateIds);
        List<Long> picked = candidateIds.subList(0, Math.min(limit, candidateIds.size()));
        return findAllById(picked);
    }

    /** 모의고사 삭제 시 편성된 문제들을 풀로 복귀시킴 */
    @Modifying
    @Query("UPDATE QuestionEntity q SET q.mockExam = null, q.displayOrder = null WHERE q.mockExam.id = :mockExamId")
    int releaseFromMockExam(@Param("mockExamId") Long mockExamId);

    /**
     * 무한 풀이 — 사용자가 푼 문제는 풀 맨 뒤로 밀려서 같은 문제 반복 노출이 줄어든다.
     * - 한 번도 안 푼 문제 우선 (recent_id IS NULL)
     * - 그 다음 가장 오래 전에 푼 문제 (recent_id ASC)
     * - 동률은 RAND()
     * - 정처기 모의고사에 편성된 문제도 풀에 노출 (findAvailableIdsBySubjectId 와 동일 정책)
     */
    @Query(value = """
            SELECT q.id FROM question q
            LEFT JOIN (
                SELECT sa.question_id, MAX(sa.id) AS recent_id
                FROM solve_answer sa
                JOIN solve s ON sa.solve_id = s.id
                WHERE s.member_id = :memberId
                GROUP BY sa.question_id
            ) recent ON recent.question_id = q.id
            LEFT JOIN mock_exam me ON me.id = q.mock_exam_id
            WHERE q.subject_id = :subjectId
              AND (q.mock_exam_id IS NULL OR me.exam_type = 'ENGINEER_PRACTICAL')
            ORDER BY (recent.recent_id IS NULL) DESC, recent.recent_id ASC, RAND()
            LIMIT :size
            """, nativeQuery = true)
    List<Long> findIdsBySubjectIdRecencyOrdered(
            @Param("subjectId") Long subjectId,
            @Param("memberId") Long memberId,
            @Param("size") int size);

    @Query("SELECT q.summary FROM QuestionEntity q WHERE q.subject.id = :subjectId AND q.summary IS NOT NULL")
    List<String> findSummariesBySubjectId(@Param("subjectId") Long subjectId);

    @Query("SELECT q FROM QuestionEntity q JOIN FETCH q.subject WHERE q.subject.id = :subjectId ORDER BY q.createdAt DESC")
    Page<QuestionEntity> findBySubjectIdWithSubject(@Param("subjectId") Long subjectId, Pageable pageable);

    @Query("SELECT q FROM QuestionEntity q JOIN FETCH q.subject ORDER BY q.createdAt DESC")
    Page<QuestionEntity> findAllWithSubject(Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    long countBySubjectIdAndTopic(Long subjectId, String topic);

    long countBySubjectId(Long subjectId);

    @Query("SELECT q FROM QuestionEntity q JOIN FETCH q.subject s LEFT JOIN FETCH s.parent WHERE q.subject.id = :subjectId ORDER BY q.id ASC")
    Page<QuestionEntity> findPublicBySubjectId(@Param("subjectId") Long subjectId, Pageable pageable);

    @Query("SELECT q.id FROM QuestionEntity q ORDER BY q.id ASC")
    List<Long> findAllPublicIds();

    @Query("SELECT q.summary FROM QuestionEntity q WHERE q.subject.id = :subjectId AND q.topic = :topic AND q.summary IS NOT NULL")
    List<String> findSummariesBySubjectIdAndTopic(@Param("subjectId") Long subjectId, @Param("topic") String topic);

    /** 정처기 다양성 검증용 — 최근 N개 문제의 정답 텍스트 (subject scoped, 최신순) */
    @Query("SELECT q.answer FROM QuestionEntity q WHERE q.subject.id = :subjectId AND q.answer IS NOT NULL ORDER BY q.id DESC")
    List<String> findRecentAnswersBySubjectId(@Param("subjectId") Long subjectId, Pageable pageable);

    /** 정처기 다양성 검증용 — 최근 N개 문제의 본문 (subject scoped, 최신순) */
    @Query("SELECT q.content FROM QuestionEntity q WHERE q.subject.id = :subjectId ORDER BY q.id DESC")
    List<String> findRecentContentsBySubjectId(@Param("subjectId") Long subjectId, Pageable pageable);

    // ----------------------------------------------------------
    // 어드민 LLM 검증용 Markdown export
    // ----------------------------------------------------------

    /**
     * 정처기 export 대상 조회. 루트 subject 이름이 "정보처리기사 실기"인 모든 문제.
     * onlyUnexported=true면 exported_at IS NULL인 것만.
     */
    @Query("""
            SELECT q FROM QuestionEntity q
            JOIN FETCH q.subject s
            LEFT JOIN s.parent p
            WHERE COALESCE(p.name, s.name) = :rootName
              AND (:onlyUnexported = false OR q.exportedAt IS NULL)
            ORDER BY q.id ASC
            """)
    List<QuestionEntity> findEngineerForExport(@Param("rootName") String rootName,
                                               @Param("onlyUnexported") boolean onlyUnexported);

    /**
     * SQLD export 대상 조회. 루트 subject 이름이 "정보처리기사 실기"가 아닌 모든 문제.
     * onlyUnexported=true면 exported_at IS NULL인 것만.
     */
    @Query("""
            SELECT q FROM QuestionEntity q
            JOIN FETCH q.subject s
            LEFT JOIN s.parent p
            WHERE COALESCE(p.name, s.name) <> :engineerRootName
              AND (:onlyUnexported = false OR q.exportedAt IS NULL)
            ORDER BY q.id ASC
            """)
    List<QuestionEntity> findSqldForExport(@Param("engineerRootName") String engineerRootName,
                                           @Param("onlyUnexported") boolean onlyUnexported);

    /** ID 목록을 일괄 export 마크 */
    @Modifying
    @Query("UPDATE QuestionEntity q SET q.exportedAt = :now WHERE q.id IN :ids")
    int markAsExported(@Param("ids") List<Long> ids, @Param("now") LocalDateTime now);

    /** 정처기 export 마크 일괄 리셋 */
    @Modifying
    @Query("""
            UPDATE QuestionEntity q SET q.exportedAt = NULL
            WHERE q.id IN (
                SELECT q2.id FROM QuestionEntity q2
                JOIN q2.subject s
                LEFT JOIN s.parent p
                WHERE COALESCE(p.name, s.name) = :rootName
            )
            """)
    int resetEngineerExportMark(@Param("rootName") String rootName);

    /** SQLD export 마크 일괄 리셋 */
    @Modifying
    @Query("""
            UPDATE QuestionEntity q SET q.exportedAt = NULL
            WHERE q.id IN (
                SELECT q2.id FROM QuestionEntity q2
                JOIN q2.subject s
                LEFT JOIN s.parent p
                WHERE COALESCE(p.name, s.name) <> :engineerRootName
            )
            """)
    int resetSqldExportMark(@Param("engineerRootName") String engineerRootName);
}

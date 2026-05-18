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

import com.sqldpass.persistent.mockexam.MockExamKind;
import com.sqldpass.persistent.mockexam.MockExamVisibility;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {

    /** 어드민 검증 카테고리별 문제 조회 (어드민 페이지의 카테고리 탭용) */
    Page<QuestionEntity> findByVerificationCategoryOrderByIdDesc(
            VerificationCategory category, Pageable pageable);

    long countByVerificationCategory(VerificationCategory category);


    /**
     * 카테고리(과목) 풀에서 사용 가능한 question id 조회.
     *
     * SQLD/정처기 모두 mockExam 편성 여부와 무관하게 모두 노출.
     * (이전 정책: SQLD는 mockExam IS NULL만 → "관리 구문" 같은 좁은 카테고리는
     *  모의고사 생성으로 풀이 고갈되어 /solve에서 문제가 안 보이는 문제 발생.
     *  사용자별 "푼 문제 후순위" 로직이 별도로 있으므로 모의고사 풀이와의
     *  체감 중복은 작음.)
     */
    /**
     * 풀이 풀 후보 — 전문가 검수(expertVerified) + PUBLISHED 모의고사에 속한 문제만.
     *  - 독립 문제(mock_exam IS NULL): 제외 (검수 단위가 모의고사라 검증 불가)
     *  - PUBLISHED + expertVerified=true: 포함
     *  - DRAFT / PREMIUM / 미검수: 제외
     */
    @Query("""
            SELECT q.id FROM QuestionEntity q
            JOIN q.mockExam m
            WHERE q.subject.id = :subjectId
              AND m.visibility = com.sqldpass.persistent.mockexam.MockExamVisibility.PUBLISHED
              AND m.expertVerified = true
            """)
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
     * 미니 모의고사 풀 조회 — 해당 과목에서 미니로 아직 복제되지 않은(includedInMiniAt IS NULL)
     * 전문가 검수 완료 원본 문제만 반환.
     * 출처 분류는 (kind, visibility) 페어로 호출자가 지정:
     *  - 기출:           kind=PAST_EXAM, visibility=null
     *  - AI 무료 풀:     kind=AI,        visibility=PUBLISHED
     *  - AI 프리미엄 풀: kind=AI,        visibility=PREMIUM
     */
    @Query("""
            SELECT q FROM QuestionEntity q
            JOIN q.mockExam m
            WHERE q.subject.id = :subjectId
              AND q.includedInMiniAt IS NULL
              AND m.expertVerified = true
              AND m.kind = :kind
              AND (:visibility IS NULL OR m.visibility = :visibility)
            """)
    List<QuestionEntity> findMiniPoolBySubjectAndSource(
            @Param("subjectId") Long subjectId,
            @Param("kind") MockExamKind kind,
            @Param("visibility") MockExamVisibility visibility);

    /** 미니 풀 복제 일괄 마킹 — N+1 dirty checking 없이 단일 UPDATE */
    @Modifying
    @Query("UPDATE QuestionEntity q SET q.includedInMiniAt = :now WHERE q.id IN :ids")
    int markIncludedInMiniInBatch(@Param("ids") List<Long> ids,
                                  @Param("now") LocalDateTime now);

    /**
     * 무한 풀이 — 사용자가 푼 문제는 풀 맨 뒤로 밀려서 같은 문제 반복 노출이 줄어든다.
     * - 한 번도 안 푼 문제 우선 (recent_id IS NULL)
     * - 그 다음 가장 오래 전에 푼 문제 (recent_id ASC)
     * - 동률은 RAND()
     * - SQLD/정처기 모두 모의고사 편성 여부와 무관하게 풀에 노출 (findAvailableIdsBySubjectId 와 동일 정책)
     */
    @Query(value = """
            SELECT q.id FROM question q
            JOIN mock_exam me ON me.id = q.mock_exam_id
            LEFT JOIN (
                SELECT sa.question_id, MAX(sa.id) AS recent_id
                FROM solve_answer sa
                JOIN solve s ON sa.solve_id = s.id
                WHERE s.member_id = :memberId
                GROUP BY sa.question_id
            ) recent ON recent.question_id = q.id
            WHERE q.subject_id = :subjectId
              AND me.visibility = 'PUBLISHED'
              AND me.expert_verified = 1
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

    /** 어드민 본문/요약 키워드 LIKE 검색 — 전체 범위. */
    @Query("""
            SELECT q FROM QuestionEntity q
            JOIN FETCH q.subject
            WHERE LOWER(q.content) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(q.summary, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY q.createdAt DESC
            """)
    Page<QuestionEntity> searchAllWithSubject(@Param("q") String q, Pageable pageable);

    /** 어드민 본문/요약 키워드 LIKE 검색 — 특정 과목 한정. */
    @Query("""
            SELECT q FROM QuestionEntity q
            JOIN FETCH q.subject
            WHERE q.subject.id = :subjectId
              AND (LOWER(q.content) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(q.summary, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY q.createdAt DESC
            """)
    Page<QuestionEntity> searchBySubjectIdWithSubject(@Param("subjectId") Long subjectId,
                                                     @Param("q") String q,
                                                     Pageable pageable);

    /**
     * 검증 대상 ID 페이징 (JOIN FETCH 없이 ID만 — Pageable과 안전하게 조합).
     * 페치는 별도 {@link #findByIdInWithSubjectAndParent(List)}로 분리.
     */
    @Query("""
            SELECT q.id FROM QuestionEntity q
            LEFT JOIN q.subject.parent p
            WHERE (:subjectId IS NULL OR q.subject.id = :subjectId OR p.id = :subjectId)
              AND (:onlyUnverified = false OR q.verifiedAt IS NULL)
            ORDER BY q.createdAt DESC
            """)
    List<Long> findIdsForVerification(@Param("subjectId") Long subjectId,
                                      @Param("onlyUnverified") boolean onlyUnverified,
                                      Pageable pageable);

    @Query("""
            SELECT q.id FROM QuestionEntity q
            JOIN q.subject s
            LEFT JOIN s.parent p
            WHERE COALESCE(p.name, s.name) = :rootName
              AND (:subjectId IS NULL OR s.id = :subjectId OR p.id = :subjectId)
              AND (:onlyUnverified = false OR q.verifiedAt IS NULL)
            ORDER BY q.createdAt DESC
            """)
    List<Long> findIdsByRootNameForVerification(@Param("rootName") String rootName,
                                                @Param("subjectId") Long subjectId,
                                                @Param("onlyUnverified") boolean onlyUnverified,
                                                Pageable pageable);

    @Query("""
            SELECT q.id FROM QuestionEntity q
            JOIN q.subject s
            LEFT JOIN s.parent p
            WHERE COALESCE(p.name, s.name) NOT IN (:excludedRootNames)
              AND (:subjectId IS NULL OR s.id = :subjectId OR p.id = :subjectId)
              AND (:onlyUnverified = false OR q.verifiedAt IS NULL)
            ORDER BY q.createdAt DESC
            """)
    List<Long> findSqldIdsForVerification(@Param("excludedRootNames") List<String> excludedRootNames,
                                          @Param("subjectId") Long subjectId,
                                          @Param("onlyUnverified") boolean onlyUnverified,
                                          Pageable pageable);

    /** 검증 페이징과 페어로 — IN 절은 Pageable이 없어 in-memory pagination 경고에서 자유롭다. */
    @Query("""
            SELECT q FROM QuestionEntity q
            JOIN FETCH q.subject s
            LEFT JOIN FETCH s.parent p
            WHERE q.id IN :ids
            """)
    List<QuestionEntity> findByIdInWithSubjectAndParent(@Param("ids") List<Long> ids);

    /** 검증 완료 일괄 마킹 — N+1 dirty checking 없이 단일 UPDATE */
    @Modifying
    @Query("UPDATE QuestionEntity q SET q.verifiedAt = :verifiedAt WHERE q.id IN :ids")
    int markVerifiedInBatch(@Param("ids") List<Long> ids,
                            @Param("verifiedAt") LocalDateTime verifiedAt);

    /**
     * 검증 트리아지 — 어떤 문제를 LLM에 먼저 보낼지 정렬.
     * 우선순위:
     *   1) 사용자 피드백(QUESTION_ERROR, NEW/IN_PROGRESS) 있는 문제
     *   2) 시도 ≥ 5 & 정답률 < 20%
     *   3) 미검증 (verified_at IS NULL)
     *   4) 그 외 (force 모드일 때만 — onlyUnverified=false)
     * 시험 필터(ENGINEER/CL1/SQLD)는 service 레이어에서 rootName 파라미터로 분기.
     */
    @Query(value = """
            SELECT q.id
            FROM question q
            LEFT JOIN subject s ON s.id = q.subject_id
            LEFT JOIN subject p ON p.id = s.parent_id
            LEFT JOIN (
                SELECT question_id, COUNT(*) AS err_cnt
                FROM feedback
                WHERE type = 'QUESTION_ERROR' AND status IN ('NEW','IN_PROGRESS')
                GROUP BY question_id
            ) fb ON fb.question_id = q.id
            LEFT JOIN (
                SELECT question_id,
                       AVG(CASE WHEN is_correct THEN 1.0 ELSE 0.0 END) AS rate,
                       COUNT(*) AS cnt
                FROM solve_answer
                GROUP BY question_id
            ) st ON st.question_id = q.id
            WHERE (:rootName IS NULL OR COALESCE(p.name, s.name) = :rootName)
              AND (:excludedRootNames IS NULL OR COALESCE(p.name, s.name) NOT IN (:excludedRootNames))
              AND (:subjectId IS NULL OR s.id = :subjectId OR p.id = :subjectId)
              AND (:onlyUnverified = false OR q.verified_at IS NULL)
            ORDER BY
              (CASE WHEN fb.err_cnt IS NOT NULL THEN 1 ELSE 0 END) DESC,
              (CASE WHEN st.cnt >= 5 AND st.rate < 0.2 THEN 1 ELSE 0 END) DESC,
              (CASE WHEN q.verified_at IS NULL THEN 1 ELSE 0 END) DESC,
              q.created_at DESC
            LIMIT :lim
            """, nativeQuery = true)
    List<Long> findTriageIdsForVerification(@Param("rootName") String rootName,
                                            @Param("excludedRootNames") List<String> excludedRootNames,
                                            @Param("subjectId") Long subjectId,
                                            @Param("onlyUnverified") boolean onlyUnverified,
                                            @Param("lim") int limit);

    @Query("""
            SELECT q.id FROM QuestionEntity q
            WHERE q.mockExam.id = :mockExamId
              AND (:onlyUnverified = false OR q.verifiedAt IS NULL)
            ORDER BY q.displayOrder ASC
            """)
    List<Long> findIdsByMockExamId(@Param("mockExamId") Long mockExamId,
                                   @Param("onlyUnverified") boolean onlyUnverified);

    /** 안드로이드 앱 스냅샷 버전 계산용 — visible 회차에 속한 문제들의 max(updatedAt). MINI 는 제외. */
    @Query("SELECT MAX(q.updatedAt) FROM QuestionEntity q " +
            "JOIN q.mockExam m " +
            "WHERE m.visibility <> com.sqldpass.persistent.mockexam.MockExamVisibility.DRAFT " +
            "  AND m.expertVerified = true " +
            "  AND m.kind <> com.sqldpass.persistent.mockexam.MockExamKind.MINI")
    java.util.Optional<LocalDateTime> findSnapshotMaxUpdatedAt();

    boolean existsByContentHash(String contentHash);

    long countByVerifiedAtIsNotNull();

    long countByVerifiedAtIsNull();

    long countByCreatedAtAfter(LocalDateTime dateTime);

    long countBySubjectIdAndTopic(Long subjectId, String topic);

    long countBySubjectId(Long subjectId);

    /**
     * 여러 과목의 question 개수를 한 번의 GROUP BY로 집계.
     * 카테고리 N+1(child별 count) 제거용.
     */
    @Query("SELECT q.subject.id, COUNT(q) FROM QuestionEntity q WHERE q.subject.id IN :subjectIds GROUP BY q.subject.id")
    List<Object[]> countBySubjectIdIn(@Param("subjectIds") List<Long> subjectIds);

    @Query("""
            SELECT q FROM QuestionEntity q
            JOIN FETCH q.subject s
            LEFT JOIN FETCH s.parent
            LEFT JOIN q.mockExam m
            WHERE q.subject.id = :subjectId
              AND (m IS NULL OR m.visibility = com.sqldpass.persistent.mockexam.MockExamVisibility.PUBLISHED)
            ORDER BY q.id ASC
            """)
    Page<QuestionEntity> findPublicBySubjectId(@Param("subjectId") Long subjectId, Pageable pageable);

    @Query("""
            SELECT q.id FROM QuestionEntity q
            LEFT JOIN q.mockExam m
            WHERE (m IS NULL OR m.visibility = com.sqldpass.persistent.mockexam.MockExamVisibility.PUBLISHED)
            ORDER BY q.id ASC
            """)
    List<Long> findAllPublicIds();

    @Query("""
            SELECT q.id FROM QuestionEntity q
            LEFT JOIN q.mockExam m
            WHERE q.subject.id IN :subjectIds
              AND (m IS NULL OR m.visibility = com.sqldpass.persistent.mockexam.MockExamVisibility.PUBLISHED)
            ORDER BY q.id ASC
            """)
    List<Long> findIdsBySubjectIdIn(@Param("subjectIds") List<Long> subjectIds);

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
     * 단일 루트(정처기/컴활) export 대상 조회.
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
     * SQLD export 대상 조회. 루트 subject 이름이 정처기/컴활 루트가 아닌 모든 문제.
     * onlyUnexported=true면 exported_at IS NULL인 것만.
     */
    @Query("""
            SELECT q FROM QuestionEntity q
            JOIN FETCH q.subject s
            LEFT JOIN s.parent p
            WHERE COALESCE(p.name, s.name) NOT IN (:excludedRootNames)
              AND (:onlyUnexported = false OR q.exportedAt IS NULL)
            ORDER BY q.id ASC
            """)
    List<QuestionEntity> findSqldForExport(@Param("excludedRootNames") List<String> excludedRootNames,
                                           @Param("onlyUnexported") boolean onlyUnexported);

    /** ID 목록을 일괄 export 마크 */
    @Modifying
    @Query("UPDATE QuestionEntity q SET q.exportedAt = :now WHERE q.id IN :ids")
    int markAsExported(@Param("ids") List<Long> ids, @Param("now") LocalDateTime now);

    /** 단일 루트(정처기/컴활) export 마크 일괄 리셋 */
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

    /** SQLD export 마크 일괄 리셋 (정처기/컴활 루트 제외) */
    @Modifying
    @Query("""
            UPDATE QuestionEntity q SET q.exportedAt = NULL
            WHERE q.id IN (
                SELECT q2.id FROM QuestionEntity q2
                JOIN q2.subject s
                LEFT JOIN s.parent p
                WHERE COALESCE(p.name, s.name) NOT IN (:excludedRootNames)
            )
            """)
    int resetSqldExportMark(@Param("excludedRootNames") List<String> excludedRootNames);
}

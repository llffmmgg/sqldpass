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

    /** 모의고사 미편성 문제의 ID만 조회 — 샘플링 후보 풀로 사용 */
    @Query("SELECT q.id FROM QuestionEntity q WHERE q.subject.id = :subjectId AND q.mockExam IS NULL")
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
}

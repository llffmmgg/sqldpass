package com.sqldpass.persistent.question;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {

    /** 모의고사에 아직 편성되지 않은 문제만 과목별로 랜덤 추출 */
    @Query(value = "SELECT * FROM question WHERE subject_id = :subjectId AND mock_exam_id IS NULL ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<QuestionEntity> findRandomBySubjectId(@Param("subjectId") Long subjectId, @Param("limit") int limit);

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

    @Query("SELECT q.summary FROM QuestionEntity q WHERE q.subject.id = :subjectId AND q.topic = :topic AND q.summary IS NOT NULL")
    List<String> findSummariesBySubjectIdAndTopic(@Param("subjectId") Long subjectId, @Param("topic") String topic);
}

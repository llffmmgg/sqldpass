package com.sqldpass.persistent.question;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {

    @Query(value = "SELECT * FROM question WHERE subject_id = :subjectId ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<QuestionEntity> findRandomBySubjectId(@Param("subjectId") Long subjectId, @Param("limit") int limit);

    @Query("SELECT q.summary FROM QuestionEntity q WHERE q.subject.id = :subjectId AND q.summary IS NOT NULL")
    List<String> findSummariesBySubjectId(@Param("subjectId") Long subjectId);
}

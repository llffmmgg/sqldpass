package com.sqldpass.persistent.mockexam;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MockExamRepository extends JpaRepository<MockExamEntity, Long> {

    @Query("SELECT MAX(m.sequence) FROM MockExamEntity m")
    Optional<Integer> findMaxSequence();

    @Query("SELECT m FROM MockExamEntity m LEFT JOIN FETCH m.questions q LEFT JOIN FETCH q.question WHERE m.id = :id")
    Optional<MockExamEntity> findByIdWithQuestions(Long id);
}

package com.sqldpass.persistent.solve;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SolveRepository extends JpaRepository<SolveEntity, Long> {

    List<SolveEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}

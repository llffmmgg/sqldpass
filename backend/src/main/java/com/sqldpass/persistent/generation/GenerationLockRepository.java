package com.sqldpass.persistent.generation;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface GenerationLockRepository extends JpaRepository<GenerationLockEntity, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GenerationLockEntity g WHERE g.id = 1")
    Optional<GenerationLockEntity> findForUpdate();
}

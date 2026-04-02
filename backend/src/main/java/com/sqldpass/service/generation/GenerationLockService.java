package com.sqldpass.service.generation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.generation.GenerationLockEntity;
import com.sqldpass.persistent.generation.GenerationLockRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GenerationLockService {

    private final GenerationLockRepository generationLockRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void acquire() {
        GenerationLockEntity lock = generationLockRepository.findForUpdate()
                .orElseThrow(() -> new IllegalStateException("generation_lock 행이 없습니다"));

        if (lock.isRunning() && !lock.isStale()) {
            throw new SqldpassException(ErrorCode.GENERATION_ALREADY_RUNNING);
        }

        lock.acquire();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release() {
        generationLockRepository.findById(1)
                .ifPresent(GenerationLockEntity::release);
    }

    @Transactional(readOnly = true)
    public boolean isRunning() {
        return generationLockRepository.findById(1)
                .map(lock -> lock.isRunning() && !lock.isStale())
                .orElse(false);
    }
}

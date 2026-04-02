package com.sqldpass.service.generation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.admin.dto.GenerationStatusResponse;
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
    public void complete(String resultJson) {
        generationLockRepository.findById(1).ifPresent(lock -> lock.complete(resultJson));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(String error) {
        generationLockRepository.findById(1).ifPresent(lock -> lock.fail(error));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reset() {
        generationLockRepository.findById(1).ifPresent(GenerationLockEntity::reset);
    }

    @Transactional(readOnly = true)
    public GenerationStatusResponse getStatus() {
        return generationLockRepository.findById(1)
                .map(lock -> {
                    if (lock.isStale()) {
                        return new GenerationStatusResponse("FAILED", "생성이 비정상적으로 오래 걸려 중단되었습니다.", lock.getStartedAt());
                    }
                    return new GenerationStatusResponse(lock.getStatus(), lock.getResult(), lock.getStartedAt());
                })
                .orElse(new GenerationStatusResponse("IDLE", null, null));
    }
}

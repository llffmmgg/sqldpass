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

    /**
     * 상태 조회. stale(30분 초과 RUNNING) 감지 시 자동으로 FAILED로 정리하여
     * 다음 폴링부터는 정상 FAILED 응답으로 고정된다. (이전: stale 응답만 하고 DB는 RUNNING 유지 → 경고 반복)
     */
    @Transactional
    public GenerationStatusResponse getStatus() {
        return generationLockRepository.findById(1)
                .map(lock -> {
                    if (lock.isStale()) {
                        String msg = "생성이 비정상적으로 오래 걸려 자동 중단되었습니다 (30분 초과).";
                        lock.fail(msg);
                        return new GenerationStatusResponse("FAILED", msg, null);
                    }
                    return new GenerationStatusResponse(lock.getStatus(), lock.getResult(), lock.getStartedAt());
                })
                .orElse(new GenerationStatusResponse("IDLE", null, null));
    }
}

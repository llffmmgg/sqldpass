package com.sqldpass.service.pdf;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamRepository;
import com.sqldpass.persistent.mockexam.MockExamVisibility;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 노출 모의고사(expert_verified=true && visibility=PUBLISHED) 의 PDF 를 백그라운드에서
 * 일괄 생성해 R2 에 캐시. 신규 추가 시점부터는 {@link MockExamPdfPrewarmer} 가 자동으로 채우지만,
 * 기존 모의고사들은 한 번 호출해서 채워야 한다.
 *
 * 동작:
 *  - 동시에 한 번만 실행 (RUNNING 중에 다시 start() 하면 409)
 *  - 단일 데몬 스레드에서 모의고사 ID 오름차순으로 직렬 처리
 *  - {@link MockExamPdfService#generate(Long)} 가 hit/miss 모두 처리하므로 같은 호출이면 멱등
 *  - 진행 상황은 {@link #status()} 로 폴링
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockExamPdfBackfillService {

    private final MockExamPdfService mockExamPdfService;
    private final MockExamRepository mockExamRepository;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "mock-exam-pdf-backfill");
        t.setDaemon(true);
        return t;
    });

    public enum State { IDLE, RUNNING, DONE }

    public record Status(
            State state,
            int total,
            int processed,
            int cached,
            int generated,
            int failed,
            Long currentMockExamId,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {}

    private final AtomicReference<Status> currentStatus =
            new AtomicReference<>(new Status(State.IDLE, 0, 0, 0, 0, 0, null, null, null));

    public Status status() {
        return currentStatus.get();
    }

    /**
     * 백필 시작. 이미 RUNNING 이면 SqldpassException(STATE_CONFLICT) 던짐.
     * 즉시 반환하고 실제 작업은 백그라운드.
     */
    @Transactional(readOnly = true)
    public Status start() {
        Status existing = currentStatus.get();
        if (existing.state() == State.RUNNING) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT,
                    "이미 PDF 백필이 진행 중입니다.");
        }

        List<Long> targetIds = mockExamRepository.findAll().stream()
                .filter(e -> e.isExpertVerified()
                        && e.getVisibility() == MockExamVisibility.PUBLISHED)
                .map(MockExamEntity::getId)
                .sorted()
                .toList();

        Status initial = new Status(
                State.RUNNING, targetIds.size(), 0, 0, 0, 0,
                null, LocalDateTime.now(), null);
        currentStatus.set(initial);

        executor.submit(() -> runBackfill(targetIds));
        return initial;
    }

    private void runBackfill(List<Long> ids) {
        int processed = 0, cached = 0, generated = 0, failed = 0;
        try {
            for (Long id : ids) {
                Status snap = currentStatus.get();
                currentStatus.set(new Status(State.RUNNING, snap.total(), processed,
                        cached, generated, failed, id, snap.startedAt(), null));
                try {
                    MockExamPdfService.PdfResult r = mockExamPdfService.generate(id);
                    if (r.cached()) cached++; else generated++;
                } catch (Exception e) {
                    failed++;
                    log.warn("PDF 백필 실패: mockExamId={}", id, e);
                }
                processed++;
            }
        } finally {
            Status snap = currentStatus.get();
            currentStatus.set(new Status(State.DONE, snap.total(), processed,
                    cached, generated, failed, null,
                    snap.startedAt(), LocalDateTime.now()));
            log.info("PDF 백필 완료: total={}, cached={}, generated={}, failed={}",
                    snap.total(), cached, generated, failed);
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}

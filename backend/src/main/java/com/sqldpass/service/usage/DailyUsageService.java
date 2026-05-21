package com.sqldpass.service.usage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.usage.DailyUsageEntity;
import com.sqldpass.persistent.usage.DailyUsageRepository;
import com.sqldpass.service.payment.SubscriptionService;

import lombok.RequiredArgsConstructor;

/**
 * 무료 회원의 일일 사용량 검사·증가 서비스.
 *
 * 활성 구독자({@link SubscriptionService#getActive(Long)} 이 Present)는 카운터 row 생성을 스킵하여
 * Focus / PASS+ / UNLIMITED 가 모두 일일 한도 면제. {@code hasPremiumAccess()} 는 Focus 를 제외한
 * PASS+ 한정 권한이라 본 면제 판정에 부적합하다.
 *
 * 동시 요청 race 는 MySQL native UPSERT(ON DUPLICATE KEY UPDATE) 의 row lock 으로 방어.
 * 한도 초과 시 {@link QuotaExceededException} 을 throw 하여 @Transactional 이 INSERT 를 롤백 →
 * daily_usage 에 잘못된 누적값이 남지 않는다.
 */
@Service
@RequiredArgsConstructor
public class DailyUsageService {

    public static final int DAILY_QUESTION_LIMIT = 30;
    public static final int DAILY_MOCK_SESSION_LIMIT = 1;
    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DailyUsageRepository repository;
    private final SubscriptionService subscriptionService;

    @Transactional
    public void consumeQuestion(Long memberId, int delta) {
        if (memberId == null) return; // 비로그인은 V62 IP 쿼터 시스템에서 처리
        if (delta <= 0) return;
        if (subscriptionService.getActive(memberId).isPresent()) return; // 활성 구독자 면제

        LocalDate today = LocalDate.now(KST);
        repository.upsertAndAdd(memberId, today, delta, 0);
        DailyUsageEntity row = repository.findByMemberIdAndUsageDate(memberId, today)
                .orElseThrow(); // UPSERT 직후라 반드시 존재
        if (row.getQuestionCount() > DAILY_QUESTION_LIMIT) {
            throw new QuotaExceededException(
                    "DAILY_QUESTION_LIMIT",
                    row.getQuestionCount(),
                    DAILY_QUESTION_LIMIT,
                    nextResetAt(today)
            );
        }
    }

    @Transactional
    public void consumeMockSession(Long memberId) {
        if (memberId == null) return;
        if (subscriptionService.getActive(memberId).isPresent()) return;

        LocalDate today = LocalDate.now(KST);
        repository.upsertAndAdd(memberId, today, 0, 1);
        DailyUsageEntity row = repository.findByMemberIdAndUsageDate(memberId, today)
                .orElseThrow();
        if (row.getMockSessionCount() > DAILY_MOCK_SESSION_LIMIT) {
            throw new QuotaExceededException(
                    "DAILY_MOCK_LIMIT",
                    row.getMockSessionCount(),
                    DAILY_MOCK_SESSION_LIMIT,
                    nextResetAt(today)
            );
        }
    }

    @Transactional(readOnly = true)
    public Quota getQuota(Long memberId) {
        LocalDate today = LocalDate.now(KST);
        LocalDateTime resetAt = nextResetAt(today);
        if (memberId == null || subscriptionService.getActive(memberId).isPresent()) {
            return Quota.unlimited(resetAt);
        }
        Optional<DailyUsageEntity> row = repository.findByMemberIdAndUsageDate(memberId, today);
        int q = row.map(DailyUsageEntity::getQuestionCount).orElse(0);
        int m = row.map(DailyUsageEntity::getMockSessionCount).orElse(0);
        return new Quota(q, DAILY_QUESTION_LIMIT, m, DAILY_MOCK_SESSION_LIMIT, resetAt);
    }

    private LocalDateTime nextResetAt(LocalDate today) {
        // KST 익일 00:00. 메모리 project_kst_naive_serialization — naive LocalDateTime, 프론트에서 +09:00 부착.
        return today.plusDays(1).atStartOfDay();
    }

    /**
     * 클라이언트가 사전 표시("오늘 18/30") 하는 데 쓰는 quota envelope.
     * 활성 구독자/비로그인은 limit 가 null → 무제한 의미.
     */
    public record Quota(
            int questionUsed,
            Integer questionLimit,
            int mockUsed,
            Integer mockLimit,
            LocalDateTime resetAt
    ) {
        public static Quota unlimited(LocalDateTime resetAt) {
            return new Quota(0, null, 0, null, resetAt);
        }
    }
}

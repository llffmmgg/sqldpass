package com.sqldpass.service.payment;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.payment.SubscriptionHistoryAction;
import com.sqldpass.persistent.payment.SubscriptionHistoryEntity;
import com.sqldpass.persistent.payment.SubscriptionHistoryRepository;
import com.sqldpass.persistent.payment.SubscriptionPlan;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 구독 이력 감사 기록기.
 *
 * 호출자 트랜잭션이 롤백되어도 audit 만은 보존되도록 REQUIRES_NEW 로 분리한다.
 * audit 저장 실패가 본 비즈니스 로직을 깨면 안 되므로 예외는 swallow + log.warn 만 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionHistoryService {

    private final SubscriptionHistoryRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long memberId, SubscriptionPlan plan, SubscriptionHistoryAction action,
                       String reason, Long actorAdminId, Long paymentId) {
        try {
            SubscriptionHistoryEntity entity = new SubscriptionHistoryEntity(
                    memberId, plan, action, reason, actorAdminId, paymentId, LocalDateTime.now());
            repository.save(entity);
        } catch (RuntimeException e) {
            log.warn("subscription_history 기록 실패 — memberId={}, plan={}, action={}, reason={}",
                    memberId, plan, action, reason, e);
        }
    }
}

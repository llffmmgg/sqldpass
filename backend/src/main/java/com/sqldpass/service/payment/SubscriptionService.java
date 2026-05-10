package com.sqldpass.service.payment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.payment.SubscriptionEntity;
import com.sqldpass.persistent.payment.SubscriptionHistoryAction;
import com.sqldpass.persistent.payment.SubscriptionPlan;
import com.sqldpass.persistent.payment.SubscriptionRepository;

import lombok.RequiredArgsConstructor;

/**
 * 회원의 활성 구독 조회 + 권한 판정.
 *
 * 모든 회원(화이트리스트 닉네임 포함) 이 동일 규칙으로 권한 판정 — DB 의 SubscriptionEntity
 * 만으로 결정. 화이트리스트는 결제 페이지 가시성(/checkout) 에만 영향을 주고, 실제 권한은
 * 결제 후 발급된 SubscriptionEntity 로만 받는다 (PaymentController.eligibility / PaymentService.ensureReviewer).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionHistoryService historyService;

    /**
     * Play Billing RTDN(refund) 또는 운영자 환불 처리 — 결제 FK 로 구독 row 를 찾아 expiresAt=now.
     * 매칭되는 구독이 없으면 no-op (이미 환불 처리됐거나 구독이 발급되지 않은 결제).
     */
    @Transactional
    public boolean revokeByPaymentId(Long paymentId) {
        if (paymentId == null) return false;
        var found = subscriptionRepository.findByPaymentId(paymentId);
        if (found.isEmpty()) return false;
        SubscriptionEntity sub = found.get();
        sub.revoke(LocalDateTime.now());
        historyService.record(sub.getMemberId(), sub.getPlan(),
                SubscriptionHistoryAction.REVOKED, "revokeByPaymentId",
                /* actorAdminId */ null, paymentId);
        return true;
    }

    /** 활성 구독 정보. 없으면 Optional.empty(). */
    public Optional<ActiveSubscription> getActive(Long memberId) {
        if (memberId == null) return Optional.empty();

        List<SubscriptionEntity> rows =
                subscriptionRepository.findActiveByMemberId(memberId, LocalDateTime.now());
        if (rows.isEmpty()) return Optional.empty();
        SubscriptionEntity top = rows.get(0);
        return Optional.of(new ActiveSubscription(
                top.getPlan(), top.getExpiresAt(),
                top.getPlan().isRemovesAds(), top.getPlan().isAllowsPdf()));
    }

    public boolean hasPremiumAccess(Long memberId) {
        return getActive(memberId).isPresent();
    }

    public boolean removesAds(Long memberId) {
        return getActive(memberId).map(ActiveSubscription::removesAds).orElse(false);
    }

    public boolean allowsPdf(Long memberId) {
        return getActive(memberId).map(ActiveSubscription::allowsPdf).orElse(false);
    }

    /** 활성 구독의 외부 노출 형태. UNLIMITED 면 expiresAt = null. */
    public record ActiveSubscription(
            SubscriptionPlan plan,
            LocalDateTime expiresAt,
            boolean removesAds,
            boolean allowsPdf
    ) {}
}

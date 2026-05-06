package com.sqldpass.service.payment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.payment.SubscriptionEntity;
import com.sqldpass.persistent.payment.SubscriptionPlan;
import com.sqldpass.persistent.payment.SubscriptionRepository;

import lombok.RequiredArgsConstructor;

/**
 * 회원의 활성 구독 조회 + 권한 판정.
 *
 * - DB 의 subscription row 를 가장 강한 plan 우선으로 정렬해 첫 번째를 활성으로 사용.
 * - sqldpass.payment.reviewer-nicknames 화이트리스트 닉네임 회원은 DB row 없이도
 *   가상 UNLIMITED 권한을 부여한다 (카드사 심사용 운영자 검토 + PDF 검증 흐름 단순화).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final MemberRepository memberRepository;
    private final PaymentProperties paymentProperties;

    /** 활성 구독 정보. 없으면 Optional.empty(). */
    public Optional<ActiveSubscription> getActive(Long memberId) {
        if (memberId == null) return Optional.empty();

        // 1) 화이트리스트 닉네임 → 가상 UNLIMITED
        if (isReviewerNickname(memberId)) {
            return Optional.of(ActiveSubscription.virtualUnlimited());
        }

        // 2) DB 의 활성 구독 (강한 plan 우선)
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

    private boolean isReviewerNickname(Long memberId) {
        var allowed = paymentProperties.reviewerNicknameSet();
        if (allowed.isEmpty()) return false;
        MemberEntity m = memberRepository.findById(memberId).orElse(null);
        return m != null && allowed.contains(m.getNickname());
    }

    /**
     * 활성 구독의 외부 노출 형태.
     * UNLIMITED 면 expiresAt = null.
     */
    public record ActiveSubscription(
            SubscriptionPlan plan,
            LocalDateTime expiresAt,
            boolean removesAds,
            boolean allowsPdf
    ) {
        public static ActiveSubscription virtualUnlimited() {
            return new ActiveSubscription(SubscriptionPlan.UNLIMITED, null, true, true);
        }
    }
}

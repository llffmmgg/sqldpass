package com.sqldpass.service.payment;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.payment.PaymentEntity;
import com.sqldpass.persistent.payment.PaymentRepository;
import com.sqldpass.persistent.payment.SubscriptionEntity;
import com.sqldpass.persistent.payment.SubscriptionPlan;
import com.sqldpass.persistent.payment.SubscriptionRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 4티어 구독 결제 — 단발 결제 → SubscriptionEntity (expiresAt = now + plan.days) 발급.
 * UNLIMITED 는 expiresAt = null.
 *
 * 화이트리스트 닉네임 회원은 결제 단계 통과만 시키고, 권한 판정은 SubscriptionService 가
 * 가상 UNLIMITED 로 자동 부여 (DB row 안 생김).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentProperties properties;
    private final PortOneClient portOneClient;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MemberRepository memberRepository;

    /**
     * 결제 직전 — client 가 PortOne 결제창을 띄우기 전에 paymentId 를 사전 등록한다.
     */
    @Transactional
    public PreparePaymentResult prepare(Long memberId, SubscriptionPlan plan) {
        ensureReviewer(memberId);
        if (plan == null) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "plan 은 필수입니다.");
        }

        PaymentProperties.PlanConfig cfg = properties.configFor(plan);
        int amount = cfg.getAmount();
        if (amount < 1) {
            throw new SqldpassException(ErrorCode.PAYMENT_VERIFICATION_FAILED,
                    "결제 금액 설정이 올바르지 않습니다.");
        }
        String productName = cfg.getProductName();
        if (productName == null || productName.isBlank() || productName.toUpperCase().contains("TEST")) {
            throw new SqldpassException(ErrorCode.PAYMENT_VERIFICATION_FAILED,
                    "결제 상품명 설정이 올바르지 않습니다.");
        }

        String paymentId = "sqldpass-" + System.currentTimeMillis() + "-" + memberId;
        PaymentEntity entity = new PaymentEntity(paymentId, memberId, null, productName, plan, amount);
        paymentRepository.save(entity);

        log.info("결제 prepare memberId={} plan={} paymentId={} amount={}",
                memberId, plan, paymentId, amount);
        return new PreparePaymentResult(
                paymentId, amount, productName, plan,
                properties.getPortone().getStoreId());
    }

    /**
     * 결제 완료 후 — PortOne REST 로 status/amount 재검증 후 SubscriptionEntity 발급.
     */
    @Transactional
    public VerifyPaymentResult verify(Long memberId, String paymentId) {
        ensureReviewer(memberId);

        PaymentEntity entity = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.PAYMENT_NOT_FOUND));
        if (!entity.getMemberId().equals(memberId)) {
            throw new SqldpassException(ErrorCode.FORBIDDEN, "본인의 결제만 검증할 수 있습니다.");
        }
        if (entity.getPlan() == null) {
            throw new SqldpassException(ErrorCode.PAYMENT_VERIFICATION_FAILED,
                    "구독 plan 정보가 없는 결제입니다.");
        }

        PortOneClient.PortOnePaymentInfo info = portOneClient.getPayment(paymentId);
        if (!info.isPaid()) {
            entity.markFailed("status=" + info.status());
            // 사용자 노출 메시지는 ErrorCode 기본값만 — raw status 는 log 로
            log.warn("결제 verify status mismatch memberId={} paymentId={} status={}",
                    memberId, paymentId, info.status());
            throw new SqldpassException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
        if (info.amountTotal() != entity.getAmount()) {
            entity.markFailed("expected=" + entity.getAmount() + " actual=" + info.amountTotal());
            // 금액 노출은 디버깅에는 유용하지만 사용자에겐 노출 X
            log.warn("결제 금액 mismatch memberId={} paymentId={} expected={} actual={}",
                    memberId, paymentId, entity.getAmount(), info.amountTotal());
            throw new SqldpassException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        LocalDateTime paidAt = info.paidAt() != null
                ? info.paidAt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                : LocalDateTime.now();
        entity.markPaid(info.raw() == null ? null : info.raw().toString(), paidAt);

        SubscriptionPlan plan = entity.getPlan();
        LocalDateTime expiresAt = plan.isLifetime() ? null : paidAt.plusDays(plan.getDays());
        SubscriptionEntity subscription = new SubscriptionEntity(
                memberId, plan, entity.getId(), paidAt, expiresAt);
        subscriptionRepository.save(subscription);

        log.info("결제 verify 성공 memberId={} paymentId={} plan={} expiresAt={}",
                memberId, paymentId, plan, expiresAt);
        return new VerifyPaymentResult(paymentId, entity.getAmount(), entity.getProductName(),
                plan, expiresAt);
    }

    private void ensureReviewer(Long memberId) {
        var allowed = properties.reviewerNicknameSet();
        if (allowed.isEmpty()) {
            // 정식 오픈 모드 — 모든 로그인 회원 통과.
            return;
        }
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MEMBER_NOT_FOUND));
        if (!allowed.contains(member.getNickname())) {
            throw new SqldpassException(ErrorCode.PAYMENT_REVIEWER_ONLY);
        }
    }

    public record PreparePaymentResult(String paymentId, int amount, String productName,
                                       SubscriptionPlan plan, String storeId) {}

    public record VerifyPaymentResult(String paymentId, int amount, String productName,
                                      SubscriptionPlan plan, LocalDateTime expiresAt) {}
}

package com.sqldpass.service.payment;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.springframework.beans.factory.annotation.Value;

import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.payment.PaymentEntity;
import com.sqldpass.persistent.payment.PaymentProvider;
import com.sqldpass.persistent.payment.PaymentRepository;
import com.sqldpass.persistent.payment.PaymentStatus;
import com.sqldpass.persistent.payment.SubscriptionEntity;
import com.sqldpass.persistent.payment.SubscriptionHistoryAction;
import com.sqldpass.persistent.payment.SubscriptionPlan;
import com.sqldpass.persistent.payment.SubscriptionRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.setting.AppSettingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 구독 결제 — 단발 결제 → SubscriptionEntity (expiresAt = now + plan.days) 발급.
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
    private final PlayBillingClient playBillingClient;
    private final PlayBillingProperties playBillingProperties;
    private final AppStoreClient appStoreClient;
    private final PaymentFailureRecorder failureRecorder;
    private final SubscriptionHistoryService historyService;
    private final SubscriptionService subscriptionService;
    private final AppSettingService appSettingService;
    private final DiscordNotifier discordNotifier;

    /** iOS 앱 Bundle ID — Apple StoreKit signedTransaction payload 의 bundleId 와 일치해야 한다. */
    @Value("${sqldpass.payment.app-store.bundle-id:com.sqldpass.app}")
    private String appStoreBundleId;

    /** 카드사 심사 가이드 + PG 정책상 최소 결제 금액. */
    private static final int MIN_CHARGE_AMOUNT = 100;

    /**
     * 미리 보기 — 회원의 활성 구독을 고려한 실제 결제 금액(prorate 적용) 계산.
     * UI 가 결제 카드에 차감 가격을 미리 표시할 때 사용. PaymentEntity 저장 X.
     */
    public PreviewResult preview(Long memberId, SubscriptionPlan plan) {
        ensureReviewer(memberId);
        if (plan == null) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "plan 은 필수입니다.");
        }
        PaymentProperties.PlanConfig cfg = properties.configFor(plan);
        int baseAmount = cfg.getAmount();

        SubscriptionEntity active = subscriptionRepository
                .findActiveByMemberId(memberId, LocalDateTime.now())
                .stream().findFirst().orElse(null);

        UpgradeEvaluation eval = evaluateUpgrade(active, plan, baseAmount);
        return new PreviewResult(plan, baseAmount, eval.discount(),
                eval.finalAmount(), eval.allowed(), eval.reason());
    }

    /**
     * 결제 직전 — client 가 PortOne 결제창을 띄우기 전에 paymentId 를 사전 등록한다.
     * 활성 구독이 있으면 prorate 차감 / 다운그레이드·동등 plan 차단 적용.
     */
    @Transactional
    public PreparePaymentResult prepare(Long memberId, SubscriptionPlan plan,
                                         String buyerName, String buyerEmail, String buyerPhoneNumber) {
        ensureReviewer(memberId);
        if (plan == null) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "plan 은 필수입니다.");
        }

        PaymentProperties.PlanConfig cfg = properties.configFor(plan);
        int baseAmount = cfg.getAmount();
        if (baseAmount < 1) {
            throw new SqldpassException(ErrorCode.PAYMENT_VERIFICATION_FAILED,
                    "결제 금액 설정이 올바르지 않습니다.");
        }
        String productName = cfg.getProductName();
        if (productName == null || productName.isBlank() || productName.toUpperCase().contains("TEST")) {
            throw new SqldpassException(ErrorCode.PAYMENT_VERIFICATION_FAILED,
                    "결제 상품명 설정이 올바르지 않습니다.");
        }

        SubscriptionEntity active = subscriptionRepository
                .findActiveByMemberId(memberId, LocalDateTime.now())
                .stream().findFirst().orElse(null);
        UpgradeEvaluation eval = evaluateUpgrade(active, plan, baseAmount);
        if (!eval.allowed()) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, eval.reason());
        }

        int finalAmount = eval.finalAmount();
        String paymentId = "sqldpass-" + System.currentTimeMillis() + "-" + memberId;
        PaymentEntity entity = new PaymentEntity(paymentId, memberId, null,
                productName, plan, finalAmount, baseAmount, eval.discount());
        // KG이니시스 PortOne V2 PC 일반결제 customer 정보 동봉 저장 (영수증·CS 식별).
        entity.setBuyer(buyerName, buyerEmail, normalizePhone(buyerPhoneNumber));
        paymentRepository.save(entity);

        log.info("결제 prepare memberId={} plan={} paymentId={} base={} discount={} final={}",
                memberId, plan, paymentId, baseAmount, eval.discount(), finalAmount);
        return new PreparePaymentResult(
                paymentId, finalAmount, productName, plan,
                properties.getPortone().getStoreId(),
                baseAmount, eval.discount());
    }

    /** 결제 customer.phoneNumber 정규화 — 하이픈·공백 제거. PG 호환성 ↑. */
    private static String normalizePhone(String raw) {
        return raw == null ? null : raw.replaceAll("[-\\s]", "");
    }

    /**
     * 활성 구독 + 새 plan 의 업그레이드 가능성 + prorate 차감액을 평가.
     * 차감액 (잔여 가치) = currentPlan 정가 × (만료까지 남은 일수 / currentPlan.days). 올림.
     */
    private UpgradeEvaluation evaluateUpgrade(SubscriptionEntity active, SubscriptionPlan newPlan, int baseAmount) {
        if (active == null) {
            // 활성 구독 없음 — 일반 결제
            return new UpgradeEvaluation(true, 0, baseAmount, null);
        }
        SubscriptionPlan currentPlan = active.getPlan();
        if (currentPlan == SubscriptionPlan.UNLIMITED) {
            return new UpgradeEvaluation(false, 0, baseAmount,
                    "이미 All Pass 를 이용 중입니다.");
        }
        if (!newPlan.isUpgradeFrom(currentPlan)) {
            // 같은/낮은 plan — 다운그레이드 또는 갱신은 만료 후
            return new UpgradeEvaluation(false, 0, baseAmount,
                    "현재 이용권이 만료된 후 결제하실 수 있습니다.");
        }
        // 업그레이드 — prorate 차감
        int discount = calculateProrateDiscount(active);
        int finalAmount = Math.max(MIN_CHARGE_AMOUNT, baseAmount - discount);
        return new UpgradeEvaluation(true, discount, finalAmount, null);
    }

    /** 활성 구독의 잔여 가치. UNLIMITED 또는 invalid 데이터면 0. */
    private int calculateProrateDiscount(SubscriptionEntity active) {
        SubscriptionPlan currentPlan = active.getPlan();
        Integer totalDays = currentPlan.getDays();
        if (totalDays == null || totalDays <= 0) return 0;
        if (active.getExpiresAt() == null) return 0;

        LocalDateTime now = LocalDateTime.now();
        if (!active.getExpiresAt().isAfter(now)) return 0;
        long remainingMillis = java.time.Duration.between(now, active.getExpiresAt()).toMillis();
        long remainingDays = (long) Math.ceil(remainingMillis / 86_400_000.0);
        if (remainingDays <= 0) return 0;
        if (remainingDays > totalDays) remainingDays = totalDays;

        int currentBaseAmount = properties.configFor(currentPlan).getAmount();
        return (int) Math.round(currentBaseAmount * (remainingDays / (double) totalDays));
    }

    private record UpgradeEvaluation(boolean allowed, int discount, int finalAmount, String reason) {}

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

        // PAID 재검증 — PortOne 재호출/구독 재발급 없이 기존 결과 그대로 반환 (idempotent).
        if (entity.getStatus() == PaymentStatus.PAID) {
            LocalDateTime cachedExpiresAt = subscriptionRepository.findByPaymentId(entity.getId())
                    .map(SubscriptionEntity::getExpiresAt).orElse(null);
            return new VerifyPaymentResult(entity.getPaymentId(), entity.getAmount(),
                    entity.getProductName(), entity.getPlan(), cachedExpiresAt);
        }

        PortOneClient.PortOnePaymentInfo info = portOneClient.getPayment(paymentId);
        if (!info.isPaid()) {
            failureRecorder.markFailedInNewTx(entity.getId(), "status=" + info.status());
            // 사용자 노출 메시지는 ErrorCode 기본값만 — raw status 는 log 로
            log.warn("결제 verify status mismatch memberId={} paymentId={} status={}",
                    memberId, paymentId, info.status());
            // 사용자 취소(CANCELLED)는 일반 실패와 구분 — 프론트가 "취소되었습니다" info 톤으로 표시.
            // FAILED / VIRTUAL_ACCOUNT_ISSUED / 기타는 기존 PAYMENT_VERIFICATION_FAILED 유지.
            if ("CANCELLED".equalsIgnoreCase(info.status())) {
                throw new SqldpassException(ErrorCode.PAYMENT_CANCELLED);
            }
            throw new SqldpassException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
        if (!"KRW".equalsIgnoreCase(info.currency())) {
            // currency 누락(null) 도 KRW 와 다름으로 간주 — 응답 누락이 무조건 통과되면 안 된다.
            failureRecorder.markFailedInNewTx(entity.getId(),
                    "currency=" + info.currency() + " expected=KRW");
            log.warn("결제 currency mismatch memberId={} paymentId={} currency={}",
                    memberId, paymentId, info.currency());
            throw new SqldpassException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
        if (info.amountTotal() != entity.getAmount()) {
            failureRecorder.markFailedInNewTx(entity.getId(),
                    "expected=" + entity.getAmount() + " actual=" + info.amountTotal());
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
        // 사용자 정책: 결제 시각의 시·분·초 무시, paidAt 의 KR 일자 + (plan.days + 1)일의 00:00 KR 에 만료.
        // 결제일 자체가 사용 가능 일자에 포함되므로 사실상 +1일 보너스.
        // 예: KR 5/14 02:00 Thunder → 5/14·5/15·5/16·5/17 사용 → 5/18 00:00 만료.
        LocalDateTime expiresAt = plan.isLifetime()
                ? null
                : paidAt.toLocalDate().plusDays(plan.getDays() + 1L).atStartOfDay();
        SubscriptionEntity subscription = new SubscriptionEntity(
                memberId, plan, entity.getId(), paidAt, expiresAt);
        subscriptionRepository.save(subscription);

        scheduleDiscordPaymentNotify(memberId, entity, subscription);

        log.info("결제 verify 성공 memberId={} paymentId={} plan={} expiresAt={}",
                memberId, paymentId, plan, expiresAt);
        return new VerifyPaymentResult(paymentId, entity.getAmount(), entity.getProductName(),
                plan, expiresAt);
    }

    /**
     * Discord 결제 알림 등록 — 트랜잭션 커밋 후 비동기 발송.
     *
     * <p>커밋 전 발송 시 rollback 케이스에서 "유령 결제 알림" 발생하므로 afterCommit 필수.
     * 알림 발송 실패는 결제 결과/응답에 영향 없게 try/catch 격리.
     * 트랜잭션 외부 호출은 즉시 발송(드문 케이스).
     */
    private void scheduleDiscordPaymentNotify(Long memberId, PaymentEntity payment, SubscriptionEntity subscription) {
        final Long memberIdSnapshot = memberId;
        final PaymentEntity paymentSnapshot = payment;
        final SubscriptionEntity subSnapshot = subscription;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendPaymentNotifySafely(memberIdSnapshot, paymentSnapshot, subSnapshot);
                }
            });
        } else {
            sendPaymentNotifySafely(memberIdSnapshot, paymentSnapshot, subSnapshot);
        }
    }

    private void sendPaymentNotifySafely(Long memberId, PaymentEntity payment, SubscriptionEntity subscription) {
        try {
            MemberEntity member = memberRepository.findById(memberId).orElse(null);
            discordNotifier.notifyPaymentComplete(member, payment, subscription);
        } catch (Exception e) {
            log.warn("결제 Discord 알림 실패 paymentId={}", payment != null ? payment.getPaymentId() : null, e);
        }
    }

    private void ensureReviewer(Long memberId) {
        if (appSettingService.isCheckoutOpenToAll()) {
            // 어드민 토글 ON — 전 회원 결제 허용 (PaymentController.eligibility 와 동일 정책).
            return;
        }
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

    /**
     * 안드로이드 앱 Google Play Billing 결제 검증 — 클라이언트가 보낸 productId/purchaseToken 으로
     * Google Play Developer API 에 영수증 검증 후 SubscriptionEntity 발급.
     *
     * <p>PortOne 흐름과 달리 prepare 단계가 없다. 결제는 디바이스 ↔ Google 사이에서 끝나고
     * 백엔드는 사후 검증만 한다. 같은 purchaseToken 재요청은 idempotent — 첫 요청만 발급.
     */
    @Transactional
    public VerifyPaymentResult verifyPlayBilling(Long memberId, String productId, String purchaseToken) {
        ensureReviewer(memberId);
        if (purchaseToken == null || purchaseToken.isBlank()) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "purchaseToken 가 비어 있습니다.");
        }

        SubscriptionPlan plan = playBillingProperties.planFor(productId);
        if (plan == null) {
            throw new SqldpassException(ErrorCode.PAYMENT_VERIFICATION_FAILED,
                    "알 수 없는 상품: " + productId);
        }

        // Idempotency — 같은 토큰이 이미 처리됐으면 같은 응답을 다시 돌려준다 (재시도 무해).
        Optional<PaymentEntity> existing = paymentRepository.findByPurchaseToken(purchaseToken);
        if (existing.isPresent() && !existing.get().getMemberId().equals(memberId)) {
            // 다른 회원의 purchaseToken 재사용 시도 — 토큰 도용 차단.
            log.warn("Play Billing 토큰 도용 시도 memberId={} expected={} token={}",
                    memberId, existing.get().getMemberId(), maskToken(purchaseToken));
            throw new SqldpassException(ErrorCode.FORBIDDEN, "다른 회원의 결제 토큰입니다.");
        }
        if (existing.isPresent() && existing.get().getStatus() == PaymentStatus.PAID) {
            PaymentEntity prior = existing.get();
            LocalDateTime priorExpires = subscriptionRepository.findByPaymentId(prior.getId())
                    .map(SubscriptionEntity::getExpiresAt).orElse(null);
            return new VerifyPaymentResult(prior.getPaymentId(), prior.getAmount(),
                    prior.getProductName(), prior.getPlan(), priorExpires);
        }

        PlayBillingClient.PlayPurchaseInfo info =
                playBillingClient.verifyProduct(productId, purchaseToken);
        if (!info.isPurchased()) {
            log.warn("Play Billing 검증 실패 memberId={} productId={} purchaseState={}",
                    memberId, productId, info.purchaseState());
            throw new SqldpassException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        PaymentProperties.PlanConfig planConfig = properties.configFor(plan);
        int baseAmount = planConfig.getAmount();
        String productName = planConfig.getProductName();

        // 정책 검증 — UNLIMITED 활성/같은 plan/다운그레이드 차단. PortOne 흐름과 동일 규칙.
        // Play Billing 은 백엔드가 차단해도 Google 결제는 이미 완료된 상태 — 운영자 수동 환불 필요.
        SubscriptionEntity active = subscriptionRepository
                .findActiveByMemberId(memberId, LocalDateTime.now())
                .stream().findFirst().orElse(null);
        UpgradeEvaluation eval = evaluateUpgrade(active, plan, baseAmount);
        if (!eval.allowed()) {
            log.warn("Play Billing 정책 차단 memberId={} productId={} reason={}",
                    memberId, productId, eval.reason());
            throw new SqldpassException(ErrorCode.INVALID_INPUT, eval.reason());
        }

        PaymentEntity payment;
        if (existing.isPresent()) {
            // PENDING/FAILED 상태에서 재시도 — 같은 row 의 상태만 갱신.
            payment = existing.get();
        } else {
            String paymentId = "play-" + System.currentTimeMillis() + "-" + memberId;
            payment = new PaymentEntity(paymentId, memberId, null, productName, plan,
                    baseAmount, baseAmount, 0,
                    PaymentProvider.PLAY_BILLING, purchaseToken);
            paymentRepository.save(payment);
        }

        LocalDateTime paidAt = LocalDateTime.ofInstant(
                info.purchasedAtInstant(), ZoneId.systemDefault());
        payment.markPaid("play:orderId=" + info.orderId(), paidAt);

        // 사용자 정책 일관 — paidAt 의 KR 일자 + (plan.days + 1)일 00:00 KR.
        LocalDateTime expiresAt = plan.isLifetime()
                ? null
                : paidAt.toLocalDate().plusDays(plan.getDays() + 1L).atStartOfDay();
        SubscriptionEntity subscription = new SubscriptionEntity(
                memberId, plan, payment.getId(), paidAt, expiresAt);
        subscriptionRepository.save(subscription);

        // acknowledge — 3일 내 미호출 시 Google 이 자동 환불하므로 검증 직후 즉시 처리.
        if (info.needsAcknowledge()) {
            playBillingClient.acknowledge(productId, purchaseToken);
        }

        scheduleDiscordPaymentNotify(memberId, payment, subscription);

        log.info("Play Billing verify 성공 memberId={} productId={} plan={} expiresAt={}",
                memberId, productId, plan, expiresAt);
        return new VerifyPaymentResult(payment.getPaymentId(), payment.getAmount(),
                payment.getProductName(), plan, expiresAt);
    }

    /**
     * iOS 앱 Apple StoreKit 2 영수증 검증 — 클라이언트가 보낸 signedTransaction(JWS) 의 payload 를
     * 파싱해 productId/transactionId 를 추출하고 SubscriptionEntity 발급.
     *
     * <p>본 phase 1차 minimal: JWS 서명 검증 생략. 출시 직전 별도 phase 에서 Apple Root CA 체인 +
     * App Store Server API 교차 확인 추가 예정.
     *
     * <p>Idempotency — 같은 transactionId 가 이미 처리됐으면 같은 응답을 다시 돌려준다 (재시도 무해).
     * purchaseToken 컬럼을 App Store transactionId 저장소로 재활용 (Play Billing 패턴과 동일).
     */
    @Transactional
    public VerifyPaymentResult verifyAppStore(Long memberId, String signedTransaction, String clientProductId) {
        ensureReviewer(memberId);
        AppStoreClient.TransactionInfo info = appStoreClient.parsePayload(signedTransaction);

        // productId 일치 검증 — 클라이언트 hint 와 payload 의 productId 가 같아야 한다.
        if (!info.productId().equals(clientProductId)) {
            log.warn("App Store productId mismatch memberId={} client={} payload={}",
                    memberId, clientProductId, info.productId());
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "productId 불일치");
        }
        // bundleId 검증 — 다른 앱이 발급한 영수증 차단.
        if (!info.bundleId().equals(appStoreBundleId)) {
            log.warn("App Store bundleId mismatch memberId={} payload={} expected={}",
                    memberId, info.bundleId(), appStoreBundleId);
            throw new SqldpassException(ErrorCode.PAYMENT_VERIFICATION_FAILED, "유효하지 않은 결제");
        }

        // Idempotency — 같은 transactionId(=purchase_token 컬럼 재활용) 가 PAID 로 이미 처리됐으면 동일 응답.
        Optional<PaymentEntity> existing = paymentRepository.findByPurchaseToken(info.transactionId());
        if (existing.isPresent() && !existing.get().getMemberId().equals(memberId)) {
            log.warn("App Store transactionId 도용 시도 memberId={} expected={} txId={}",
                    memberId, existing.get().getMemberId(), maskToken(info.transactionId()));
            throw new SqldpassException(ErrorCode.FORBIDDEN, "다른 회원의 결제 영수증입니다.");
        }
        if (existing.isPresent() && existing.get().getStatus() == PaymentStatus.PAID) {
            PaymentEntity prior = existing.get();
            LocalDateTime priorExpires = subscriptionRepository.findByPaymentId(prior.getId())
                    .map(SubscriptionEntity::getExpiresAt).orElse(null);
            return new VerifyPaymentResult(prior.getPaymentId(), prior.getAmount(),
                    prior.getProductName(), prior.getPlan(), priorExpires);
        }

        return createAppStorePayment(memberId, info);
    }

    /**
     * App Store 결제 신규 발급 — 실제 영속화 + SubscriptionEntity 발급은 본 step 의 후속 PR 에서
     * {@link #verifyPlayBilling(Long, String, String)} 흐름을 미러링해 구현 예정.
     *
     * <p>현 step 은 컴파일·라우팅 골격만 완성. 호출 시 UnsupportedOperationException 발생.
     */
    private VerifyPaymentResult createAppStorePayment(Long memberId, AppStoreClient.TransactionInfo info) {
        throw new UnsupportedOperationException(
                "createAppStorePayment 실제 구현은 후속 step 에서 verifyPlayBilling 흐름을 미러링해 작성. "
                + "memberId=" + memberId + " txId=" + maskToken(info.transactionId()));
    }

    /**
     * 관리자 환불 — PortOne PG cancel + PaymentEntity CANCELLED + 구독 회수 + history REFUNDED.
     *
     * <p>본 메서드는 PortOne 채널 전용. Play Billing 환불은 RTDN 으로 자동 처리되므로
     * {@link #revokePlayBillingByToken(String)} 가 별도 담당.
     *
     * <p>이미 CANCELLED 상태면 idempotent — 호출 0회.
     *
     * @param paymentEntityId PaymentEntity 의 PK (paymentId 문자열이 아니라 DB id)
     * @param reason          환불 사유 (Discord/audit 에 기록)
     * @param actorAdminId    환불 실행 관리자 memberId (history audit)
     */
    @Transactional
    public void revokePortOnePayment(Long paymentEntityId, String reason, Long actorAdminId) {
        PaymentEntity entity = paymentRepository.findById(paymentEntityId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.PAYMENT_NOT_FOUND));
        if (entity.getProvider() != PaymentProvider.PORTONE) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT,
                    "PortOne 결제만 본 메서드로 환불 가능합니다.");
        }
        if (entity.getStatus() == PaymentStatus.CANCELLED) {
            return; // idempotent — 중복 호출 무해
        }
        // 업그레이드로 superseded 된 결제 환불 차단 — 옛 결제 환불 시 활성 구독은 최신 결제에
        // 묶여있어 사용자가 환불 + 서비스 이용 동시 발생. paidAt null 인 결제는 아직 PAID 가
        // 아니라 다른 분기에서 처리됨.
        if (entity.getPaidAt() != null && paymentRepository.existsNewerActivePaidPaymentForMember(
                entity.getMemberId(), entity.getId(), entity.getPaidAt())) {
            log.warn("superseded payment 환불 거절 paymentId={} memberId={} actorAdminId={}",
                    entity.getPaymentId(), entity.getMemberId(), actorAdminId);
            throw new SqldpassException(ErrorCode.PAYMENT_SUPERSEDED_BY_NEWER);
        }
        // PG 측 환불 — 실패 시 SqldpassException(PAYMENT_GATEWAY_ERROR) throw → 트랜잭션 롤백.
        portOneClient.cancel(entity.getPaymentId(), reason);
        entity.markCancelled("admin-refund:" + (reason == null ? "" : reason));
        // 구독 회수 + history REVOKED 자동 기록.
        subscriptionService.revokeByPaymentId(entity.getId());
        // REFUNDED action 명시 기록 — REVOKED 와 별도 한 줄.
        historyService.record(entity.getMemberId(), entity.getPlan(),
                SubscriptionHistoryAction.REFUNDED, reason, actorAdminId, entity.getId());
        log.info("PortOne 환불 paymentId={} memberId={} actorAdminId={} reason={}",
                entity.getPaymentId(), entity.getMemberId(), actorAdminId, reason);
    }

    /**
     * 결제 후 권한 미부여 복구 — PAID 결제인데 SubscriptionEntity 가 없는 비정상 상태를
     * 운영자가 명시적으로 재발급. verify 의 PAID 가드 분기는 자동 보완하지 않고
     * 본 메서드만 SubscriptionEntity 를 생성한다.
     *
     * <p>이미 활성 구독이 존재하면 idempotent — save 없이 기존 expiresAt 반환.
     * existing 이 expired 상태라면 V80 UNIQUE 제약 충돌을 피하기 위해 새 row 를 만들지 않는다
     * (별 정책 결정 필요 — 운영자가 새 결제 또는 expireManual 후 재시도).
     */
    @Transactional
    public ReissueResult reissueSubscription(Long paymentEntityId, Long actorAdminId) {
        PaymentEntity entity = paymentRepository.findById(paymentEntityId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.PAYMENT_NOT_FOUND));
        if (entity.getStatus() != PaymentStatus.PAID) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "PAID 상태인 결제만 재발급 대상입니다.");
        }
        SubscriptionPlan plan = entity.getPlan();
        if (plan == null) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "plan 정보 없는 결제는 재발급 불가합니다.");
        }
        Optional<SubscriptionEntity> existing =
                subscriptionRepository.findByPaymentId(entity.getId());
        if (existing.isPresent() && existing.get().isActive(LocalDateTime.now())) {
            return new ReissueResult(false, existing.get().getExpiresAt());
        }

        LocalDateTime paidAt = entity.getPaidAt() != null ? entity.getPaidAt() : LocalDateTime.now();
        LocalDateTime expiresAt = plan.isLifetime() ? null : paidAt.plusDays(plan.getDays());
        SubscriptionEntity subscription = new SubscriptionEntity(
                entity.getMemberId(), plan, entity.getId(), paidAt, expiresAt);
        subscriptionRepository.save(subscription);

        historyService.record(entity.getMemberId(), plan,
                SubscriptionHistoryAction.GRANTED,
                "admin-reissue:paymentId=" + entity.getPaymentId(),
                actorAdminId, entity.getId());
        log.info("결제 후 구독 재발급 paymentId={} memberId={} actorAdminId={} plan={} expiresAt={}",
                entity.getPaymentId(), entity.getMemberId(), actorAdminId, plan, expiresAt);
        return new ReissueResult(true, expiresAt);
    }

    public record ReissueResult(boolean issued, LocalDateTime expiresAt) {}

    /**
     * Play RTDN(Real-time Developer Notifications) — 환불/구독 만료 통지 처리.
     * purchaseToken 으로 결제 row 를 찾아 PaymentStatus=CANCELLED + Subscription.expiresAt=now.
     * 매칭되는 결제가 없으면 무시 (다른 앱의 알림이거나 이미 처리됨).
     */
    @Transactional
    public boolean revokePlayBillingByToken(String purchaseToken) {
        return revokeByProviderToken(PaymentProvider.PLAY_BILLING, purchaseToken, "play:rtdn-refund");
    }

    /**
     * App Store Server Notifications V2 — REFUND/REVOKE 통지 처리.
     * transactionId(purchase_token 컬럼 재활용) 로 결제 row 를 찾아 PaymentStatus=CANCELLED +
     * Subscription.expiresAt=now. 매칭되는 결제가 없으면 무시 (다른 앱의 알림이거나 이미 처리됨).
     */
    @Transactional
    public boolean revokeAppStoreByTransactionId(String transactionId) {
        return revokeByProviderToken(PaymentProvider.APP_STORE, transactionId, "appstore:refund");
    }

    /**
     * 공통 환불 회수 — purchaseToken 또는 App Store transactionId 로 결제 row + 구독 회수.
     *
     * <p>이미 CANCELLED 면 결제 markCancelled 는 idempotent skip. Subscription row 가 없거나
     * 이미 expiresAt 이 과거여도 revoke(now) 자체는 안전(멱등). history 는 매 호출마다 1줄 기록 —
     * Pub/Sub / ASSN 중복 전송 추적용.
     */
    private boolean revokeByProviderToken(PaymentProvider provider, String token, String reason) {
        if (token == null || token.isBlank()) return false;
        Optional<PaymentEntity> found = paymentRepository.findByPurchaseToken(token);
        if (found.isEmpty()) {
            log.warn("{} 환불 webhook — 결제 매칭 실패 token={}", provider, maskToken(token));
            return false;
        }

        PaymentEntity payment = found.get();
        if (provider != null && payment.getProvider() != provider) {
            log.warn("{} 환불 webhook — provider 불일치 token={} expected={} actual={}",
                    provider, maskToken(token), provider, payment.getProvider());
            return false;
        }
        if (payment.getStatus() != PaymentStatus.CANCELLED) {
            payment.markCancelled(reason);
        }
        Optional<SubscriptionEntity> sub = subscriptionRepository.findByPaymentId(payment.getId());
        if (sub.isEmpty()) return false;
        sub.get().revoke(LocalDateTime.now());
        historyService.record(payment.getMemberId(), payment.getPlan(),
                SubscriptionHistoryAction.REFUNDED, reason,
                /* actorAdminId */ null, payment.getId());
        log.info("{} revoke memberId={} paymentId={} plan={}",
                provider, payment.getMemberId(), payment.getPaymentId(), payment.getPlan());
        return true;
    }

    private static String maskToken(String token) {
        if (token == null) return "null";
        return token.length() <= 8 ? "***" : token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    public record PreparePaymentResult(String paymentId, int amount, String productName,
                                       SubscriptionPlan plan, String storeId,
                                       int baseAmount, int prorateDiscount) {}

    public record VerifyPaymentResult(String paymentId, int amount, String productName,
                                      SubscriptionPlan plan, LocalDateTime expiresAt) {}

    /**
     * /checkout 카드별 미리 보기 응답.
     * - allowed=false 면 결제 불가 (다운그레이드/UNLIMITED 활성), reason 표시.
     * - allowed=true + prorateDiscount>0 면 업그레이드 (원가 - 차감 = finalAmount).
     */
    public record PreviewResult(SubscriptionPlan plan, int baseAmount, int prorateDiscount,
                                int finalAmount, boolean allowed, String reason) {}
}

package com.sqldpass.service.payment;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import com.sqldpass.persistent.payment.SubscriptionEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.payment.PaymentEntity;
import com.sqldpass.persistent.payment.PaymentRepository;
import com.sqldpass.persistent.payment.SubscriptionEntity;
import com.sqldpass.persistent.payment.SubscriptionPlan;
import com.sqldpass.persistent.payment.SubscriptionRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PortOneClient portOneClient;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PlayBillingClient playBillingClient;
    @Mock
    private PaymentFailureRecorder failureRecorder;
    @Mock
    private SubscriptionHistoryService historyService;

    private PaymentProperties properties;
    private PlayBillingProperties playBillingProperties;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        properties = new PaymentProperties();
        properties.setReviewerNicknames("pay-rv-7f2a91");
        properties.setThreeDay(new PaymentProperties.PlanConfig(3900, "문어CBT 3일 이용권"));
        properties.setOneMonth(new PaymentProperties.PlanConfig(9900, "문어CBT 한달 이용권"));
        properties.setUnlimited(new PaymentProperties.PlanConfig(29900, "문어CBT 평생 무제한 이용권"));

        playBillingProperties = new PlayBillingProperties();
        playBillingProperties.getProductIdMapping().put(SubscriptionPlan.THREE_DAY, "iap_three_day");
        playBillingProperties.getProductIdMapping().put(SubscriptionPlan.ONE_MONTH, "iap_one_month");
        playBillingProperties.getProductIdMapping().put(SubscriptionPlan.UNLIMITED, "iap_unlimited");

        service = new PaymentService(properties, portOneClient,
                paymentRepository, subscriptionRepository, memberRepository,
                playBillingClient, playBillingProperties, failureRecorder, historyService);
    }

    @Test
    @DisplayName("화이트리스트 비어있으면 정식 오픈 모드 — 어떤 회원이든 prepare 통과")
    void prepareAllowsEveryoneWhenWhitelistEmpty() {
        properties.setReviewerNicknames("");

        var result = service.prepare(1L, SubscriptionPlan.THREE_DAY);

        assertThat(result.amount()).isEqualTo(3900);
        assertThat(result.plan()).isEqualTo(SubscriptionPlan.THREE_DAY);
        verify(paymentRepository, times(1)).save(any(PaymentEntity.class));
    }

    @Test
    @DisplayName("화이트리스트에 없는 닉네임이면 prepare 시 PAYMENT_REVIEWER_ONLY")
    void prepareBlocksNonReviewer() {
        MemberEntity m = newMember(1L, "normal-user");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));
        assertThatThrownBy(() -> service.prepare(1L, SubscriptionPlan.THREE_DAY))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_REVIEWER_ONLY);
    }

    @Test
    @DisplayName("화이트리스트 닉네임 + ONE_MONTH plan 이면 prepare 성공")
    void prepareSucceedsForReviewer() {
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));

        var result = service.prepare(1L, SubscriptionPlan.ONE_MONTH);

        assertThat(result.amount()).isEqualTo(9900);
        assertThat(result.productName()).isEqualTo("문어CBT 한달 이용권");
        assertThat(result.plan()).isEqualTo(SubscriptionPlan.ONE_MONTH);
        assertThat(result.paymentId()).startsWith("sqldpass-");
        verify(paymentRepository, times(1)).save(any(PaymentEntity.class));
    }

    @Test
    @DisplayName("plan = null 이면 INVALID_INPUT")
    void prepareRejectsNullPlan() {
        properties.setReviewerNicknames("");

        assertThatThrownBy(() -> service.prepare(1L, null))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("ONE_MONTH 활성 (15일 남음) + UNLIMITED 업그레이드 → prorate 차감 ₩4,950 적용")
    void prepareUnlimitedUpgradeApplyProrate() {
        properties.setReviewerNicknames("");
        SubscriptionEntity active = new SubscriptionEntity(
                1L, SubscriptionPlan.ONE_MONTH, 100L,
                LocalDateTime.now().minusDays(15), LocalDateTime.now().plusDays(15));
        given(subscriptionRepository.findActiveByMemberId(any(), any()))
                .willReturn(java.util.List.of(active));

        var result = service.prepare(1L, SubscriptionPlan.UNLIMITED);

        // 잔여 15일 / 30일 × ₩9,900 = ₩4,950 차감 → ₩29,900 - ₩4,950 = ₩24,950
        assertThat(result.baseAmount()).isEqualTo(29900);
        assertThat(result.prorateDiscount()).isEqualTo(4950);
        assertThat(result.amount()).isEqualTo(24950);
    }

    @Test
    @DisplayName("UNLIMITED 활성 + 어떤 plan 결제 시도 → INVALID_INPUT")
    void prepareBlockedWhenUnlimitedActive() {
        properties.setReviewerNicknames("");
        SubscriptionEntity active = new SubscriptionEntity(
                1L, SubscriptionPlan.UNLIMITED, 100L, LocalDateTime.now(), null);
        given(subscriptionRepository.findActiveByMemberId(any(), any()))
                .willReturn(java.util.List.of(active));

        assertThatThrownBy(() -> service.prepare(1L, SubscriptionPlan.ONE_MONTH))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("ONE_MONTH 활성 + 같은 ONE_MONTH 결제 → INVALID_INPUT (만료 후 가능)")
    void prepareBlockedSamePlan() {
        properties.setReviewerNicknames("");
        SubscriptionEntity active = new SubscriptionEntity(
                1L, SubscriptionPlan.ONE_MONTH, 100L,
                LocalDateTime.now().minusDays(5), LocalDateTime.now().plusDays(25));
        given(subscriptionRepository.findActiveByMemberId(any(), any()))
                .willReturn(java.util.List.of(active));

        assertThatThrownBy(() -> service.prepare(1L, SubscriptionPlan.ONE_MONTH))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("ONE_MONTH 활성 + THREE_DAY 다운그레이드 시도 → INVALID_INPUT")
    void prepareBlockedDowngrade() {
        properties.setReviewerNicknames("");
        SubscriptionEntity active = new SubscriptionEntity(
                1L, SubscriptionPlan.ONE_MONTH, 100L,
                LocalDateTime.now().minusDays(5), LocalDateTime.now().plusDays(25));
        given(subscriptionRepository.findActiveByMemberId(any(), any()))
                .willReturn(java.util.List.of(active));

        assertThatThrownBy(() -> service.prepare(1L, SubscriptionPlan.THREE_DAY))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("preview — 활성 구독 없으면 baseAmount 그대로, allowed=true")
    void previewWithoutSubscription() {
        properties.setReviewerNicknames("");
        var preview = service.preview(1L, SubscriptionPlan.ONE_MONTH);

        assertThat(preview.allowed()).isTrue();
        assertThat(preview.baseAmount()).isEqualTo(9900);
        assertThat(preview.prorateDiscount()).isEqualTo(0);
        assertThat(preview.finalAmount()).isEqualTo(9900);
    }

    @Test
    @DisplayName("preview — UNLIMITED 활성이면 allowed=false")
    void previewUnlimitedActive() {
        properties.setReviewerNicknames("");
        SubscriptionEntity active = new SubscriptionEntity(
                1L, SubscriptionPlan.UNLIMITED, 100L, LocalDateTime.now(), null);
        given(subscriptionRepository.findActiveByMemberId(any(), any()))
                .willReturn(java.util.List.of(active));

        var preview = service.preview(1L, SubscriptionPlan.ONE_MONTH);
        assertThat(preview.allowed()).isFalse();
        assertThat(preview.reason()).contains("무제한");
    }

    @Test
    @DisplayName("verify: PortOne 에서 받은 amount 가 prepare 와 다르면 PAYMENT_AMOUNT_MISMATCH")
    void verifyDetectsAmountTampering() {
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));

        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "상품", SubscriptionPlan.THREE_DAY, 3900);
        given(paymentRepository.findByPaymentId("p-1")).willReturn(Optional.of(entity));

        var info = new PortOneClient.PortOnePaymentInfo("p-1", "PAID", 9999, "KRW",
                OffsetDateTime.now(), Map.of("id", "p-1", "status", "PAID"));
        given(portOneClient.getPayment("p-1")).willReturn(info);

        assertThatThrownBy(() -> service.verify(1L, "p-1"))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }

    @Test
    @DisplayName("verify: PortOne status 가 PAID 가 아니면 PAYMENT_VERIFICATION_FAILED")
    void verifyDetectsNonPaidStatus() {
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));

        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "상품", SubscriptionPlan.THREE_DAY, 3900);
        given(paymentRepository.findByPaymentId("p-1")).willReturn(Optional.of(entity));

        var info = new PortOneClient.PortOnePaymentInfo("p-1", "READY", 3900, "KRW",
                null, Map.of("id", "p-1", "status", "READY"));
        given(portOneClient.getPayment("p-1")).willReturn(info);

        assertThatThrownBy(() -> service.verify(1L, "p-1"))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_VERIFICATION_FAILED);
    }

    @Test
    @DisplayName("THREE_DAY verify 성공 시 SubscriptionEntity 가 expiresAt = now+3d 로 발급")
    void verifyThreeDayCreatesSubscription() {
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));

        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "3일 이용권",
                SubscriptionPlan.THREE_DAY, 3900);
        given(paymentRepository.findByPaymentId("p-1")).willReturn(Optional.of(entity));

        OffsetDateTime paidAt = OffsetDateTime.now();
        var info = new PortOneClient.PortOnePaymentInfo("p-1", "PAID", 3900, "KRW",
                paidAt, Map.of("id", "p-1", "status", "PAID"));
        given(portOneClient.getPayment("p-1")).willReturn(info);

        var result = service.verify(1L, "p-1");

        assertThat(result.plan()).isEqualTo(SubscriptionPlan.THREE_DAY);
        assertThat(result.expiresAt()).isNotNull();

        ArgumentCaptor<SubscriptionEntity> captor = ArgumentCaptor.forClass(SubscriptionEntity.class);
        verify(subscriptionRepository, times(1)).save(captor.capture());
        SubscriptionEntity saved = captor.getValue();
        assertThat(saved.getMemberId()).isEqualTo(1L);
        assertThat(saved.getPlan()).isEqualTo(SubscriptionPlan.THREE_DAY);
        assertThat(saved.getExpiresAt()).isEqualToIgnoringSeconds(saved.getPurchasedAt().plusDays(3));
    }

    @Test
    @DisplayName("verify: 이미 PAID 상태 재호출 시 PortOne·subscription save 없이 기존 결과 반환 (idempotent)")
    void verifyIsIdempotentWhenAlreadyPaid() {
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));

        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "3일 이용권",
                SubscriptionPlan.THREE_DAY, 3900);
        LocalDateTime paidAt = LocalDateTime.now().minusMinutes(1);
        entity.markPaid("{...}", paidAt);
        setField(entity, "id", 42L);
        given(paymentRepository.findByPaymentId("p-1")).willReturn(Optional.of(entity));

        SubscriptionEntity prior = new SubscriptionEntity(
                1L, SubscriptionPlan.THREE_DAY, 42L, paidAt, paidAt.plusDays(3));
        given(subscriptionRepository.findByPaymentId(42L)).willReturn(Optional.of(prior));

        var result = service.verify(1L, "p-1");

        assertThat(result.paymentId()).isEqualTo("p-1");
        assertThat(result.plan()).isEqualTo(SubscriptionPlan.THREE_DAY);
        assertThat(result.expiresAt()).isEqualTo(prior.getExpiresAt());

        verify(portOneClient, times(0)).getPayment(any());
        verify(subscriptionRepository, times(0)).save(any(SubscriptionEntity.class));
        verify(failureRecorder, times(0)).markFailedInNewTx(any(), any());
    }

    @Test
    @DisplayName("verify: currency 가 KRW 아니면 PAYMENT_AMOUNT_MISMATCH + markFailedInNewTx 호출")
    void verifyDetectsNonKrwCurrency() {
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));

        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "상품",
                SubscriptionPlan.THREE_DAY, 3900);
        setField(entity, "id", 42L);
        given(paymentRepository.findByPaymentId("p-1")).willReturn(Optional.of(entity));

        var info = new PortOneClient.PortOnePaymentInfo("p-1", "PAID", 3900, "USD",
                OffsetDateTime.now(), Map.of("id", "p-1", "status", "PAID"));
        given(portOneClient.getPayment("p-1")).willReturn(info);

        assertThatThrownBy(() -> service.verify(1L, "p-1"))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);

        verify(failureRecorder, times(1)).markFailedInNewTx(eq(42L), anyString());
        verify(subscriptionRepository, times(0)).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("verify: status mismatch 시 markFailedInNewTx 호출 — REQUIRES_NEW 경로 검증")
    void verifyStatusMismatchInvokesFailureRecorder() {
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));

        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "상품",
                SubscriptionPlan.THREE_DAY, 3900);
        setField(entity, "id", 42L);
        given(paymentRepository.findByPaymentId("p-1")).willReturn(Optional.of(entity));

        var info = new PortOneClient.PortOnePaymentInfo("p-1", "READY", 3900, "KRW",
                null, Map.of("id", "p-1", "status", "READY"));
        given(portOneClient.getPayment("p-1")).willReturn(info);

        assertThatThrownBy(() -> service.verify(1L, "p-1"))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_VERIFICATION_FAILED);

        verify(failureRecorder, times(1)).markFailedInNewTx(eq(42L), anyString());
        verify(subscriptionRepository, times(0)).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("verify: status=FAILED 면 PAYMENT_VERIFICATION_FAILED + markFailedInNewTx 호출")
    void verifyStatusFailedInvokesFailureRecorder() {
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));

        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "상품",
                SubscriptionPlan.THREE_DAY, 3900);
        setField(entity, "id", 42L);
        given(paymentRepository.findByPaymentId("p-1")).willReturn(Optional.of(entity));

        var info = new PortOneClient.PortOnePaymentInfo("p-1", "FAILED", 3900, "KRW",
                null, Map.of("id", "p-1", "status", "FAILED"));
        given(portOneClient.getPayment("p-1")).willReturn(info);

        assertThatThrownBy(() -> service.verify(1L, "p-1"))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_VERIFICATION_FAILED);

        verify(failureRecorder, times(1)).markFailedInNewTx(eq(42L), contains("FAILED"));
        verify(subscriptionRepository, times(0)).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("verify: status=CANCELLED 면 PAYMENT_VERIFICATION_FAILED + markFailedInNewTx 호출")
    void verifyStatusCancelledInvokesFailureRecorder() {
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));

        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "상품",
                SubscriptionPlan.THREE_DAY, 3900);
        setField(entity, "id", 42L);
        given(paymentRepository.findByPaymentId("p-1")).willReturn(Optional.of(entity));

        var info = new PortOneClient.PortOnePaymentInfo("p-1", "CANCELLED", 3900, "KRW",
                null, Map.of("id", "p-1", "status", "CANCELLED"));
        given(portOneClient.getPayment("p-1")).willReturn(info);

        assertThatThrownBy(() -> service.verify(1L, "p-1"))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_VERIFICATION_FAILED);

        verify(failureRecorder, times(1)).markFailedInNewTx(eq(42L), contains("CANCELLED"));
        verify(subscriptionRepository, times(0)).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("verify: status=VIRTUAL_ACCOUNT_ISSUED (미입금) 면 PAYMENT_VERIFICATION_FAILED — 별도 PENDING 처리 없음")
    void verifyStatusVirtualAccountIssuedFails() {
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));

        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "상품",
                SubscriptionPlan.THREE_DAY, 3900);
        setField(entity, "id", 42L);
        given(paymentRepository.findByPaymentId("p-1")).willReturn(Optional.of(entity));

        var info = new PortOneClient.PortOnePaymentInfo("p-1", "VIRTUAL_ACCOUNT_ISSUED", 3900, "KRW",
                null, Map.of("id", "p-1", "status", "VIRTUAL_ACCOUNT_ISSUED"));
        given(portOneClient.getPayment("p-1")).willReturn(info);

        assertThatThrownBy(() -> service.verify(1L, "p-1"))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_VERIFICATION_FAILED);

        verify(failureRecorder, times(1)).markFailedInNewTx(eq(42L), contains("VIRTUAL_ACCOUNT_ISSUED"));
        verify(subscriptionRepository, times(0)).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("verify: PortOne 게이트웨이 5xx → PAYMENT_GATEWAY_ERROR 전파 + markFailedInNewTx 0회")
    void verifyPortOneGatewayErrorPropagates() {
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));

        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "상품",
                SubscriptionPlan.THREE_DAY, 3900);
        setField(entity, "id", 42L);
        given(paymentRepository.findByPaymentId("p-1")).willReturn(Optional.of(entity));

        given(portOneClient.getPayment("p-1"))
                .willThrow(new SqldpassException(ErrorCode.PAYMENT_GATEWAY_ERROR));

        assertThatThrownBy(() -> service.verify(1L, "p-1"))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_GATEWAY_ERROR);

        // 게이트웨이 자체 오류 — 응답 정보가 없어 markFailedInNewTx 호출 없음.
        verify(failureRecorder, times(0)).markFailedInNewTx(any(), any());
        verify(subscriptionRepository, times(0)).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("UNLIMITED verify 성공 시 expiresAt = null (평생)")
    void verifyUnlimitedCreatesLifetimeSubscription() {
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));

        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "무제한권",
                SubscriptionPlan.UNLIMITED, 29900);
        given(paymentRepository.findByPaymentId("p-1")).willReturn(Optional.of(entity));

        var info = new PortOneClient.PortOnePaymentInfo("p-1", "PAID", 29900, "KRW",
                OffsetDateTime.now(), Map.of("id", "p-1", "status", "PAID"));
        given(portOneClient.getPayment("p-1")).willReturn(info);

        var result = service.verify(1L, "p-1");

        assertThat(result.plan()).isEqualTo(SubscriptionPlan.UNLIMITED);
        assertThat(result.expiresAt()).isNull();

        ArgumentCaptor<SubscriptionEntity> captor = ArgumentCaptor.forClass(SubscriptionEntity.class);
        verify(subscriptionRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getExpiresAt()).isNull();
    }

    // ============================================================
    // Play Billing
    // ============================================================

    @Test
    @DisplayName("verifyPlayBilling: 알 수 없는 productId → PAYMENT_VERIFICATION_FAILED")
    void verifyPlayBillingRejectsUnknownProductId() {
        properties.setReviewerNicknames("");
        assertThatThrownBy(() ->
                service.verifyPlayBilling(1L, "iap_unknown", "tok-123"))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_VERIFICATION_FAILED);
    }

    @Test
    @DisplayName("verifyPlayBilling: purchaseToken 비어있으면 INVALID_INPUT")
    void verifyPlayBillingRejectsBlankToken() {
        properties.setReviewerNicknames("");
        assertThatThrownBy(() ->
                service.verifyPlayBilling(1L, "iap_three_day", ""))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("verifyPlayBilling: Google purchaseState != 0 이면 PAYMENT_VERIFICATION_FAILED")
    void verifyPlayBillingDetectsCanceled() {
        properties.setReviewerNicknames("");
        given(paymentRepository.findByPurchaseToken("tok-123")).willReturn(Optional.empty());
        var info = new PlayBillingClient.PlayPurchaseInfo(1, 0, System.currentTimeMillis(),
                "GPA-test", "KR");
        given(playBillingClient.verifyProduct("iap_three_day", "tok-123")).willReturn(info);

        assertThatThrownBy(() ->
                service.verifyPlayBilling(1L, "iap_three_day", "tok-123"))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_VERIFICATION_FAILED);
    }

    @Test
    @DisplayName("verifyPlayBilling: 신규 token + PURCHASED → 결제+구독 발급 + acknowledge 호출")
    void verifyPlayBillingCreatesSubscriptionAndAcknowledges() {
        properties.setReviewerNicknames("");
        given(paymentRepository.findByPurchaseToken("tok-fresh")).willReturn(Optional.empty());
        var info = new PlayBillingClient.PlayPurchaseInfo(0, 0, System.currentTimeMillis(),
                "GPA-test", "KR");
        given(playBillingClient.verifyProduct("iap_one_month", "tok-fresh")).willReturn(info);

        var result = service.verifyPlayBilling(1L, "iap_one_month", "tok-fresh");

        assertThat(result.plan()).isEqualTo(SubscriptionPlan.ONE_MONTH);
        assertThat(result.amount()).isEqualTo(9900);
        assertThat(result.expiresAt()).isNotNull();

        verify(paymentRepository, times(1)).save(any(PaymentEntity.class));
        ArgumentCaptor<SubscriptionEntity> subCaptor = ArgumentCaptor.forClass(SubscriptionEntity.class);
        verify(subscriptionRepository, times(1)).save(subCaptor.capture());
        assertThat(subCaptor.getValue().getPlan()).isEqualTo(SubscriptionPlan.ONE_MONTH);

        // acknowledgementState=0 이었으니 ack 호출 1회.
        verify(playBillingClient, times(1)).acknowledge("iap_one_month", "tok-fresh");
    }

    @Test
    @DisplayName("verifyPlayBilling: 같은 token 재요청은 idempotent — 새 결제 row 안 만들고 기존 결과 반환")
    void verifyPlayBillingIsIdempotentOnSameToken() {
        properties.setReviewerNicknames("");
        PaymentEntity prior = new PaymentEntity(
                "play-prior", 1L, null, "문어CBT 3일 이용권",
                SubscriptionPlan.THREE_DAY, 3900, 3900, 0,
                com.sqldpass.persistent.payment.PaymentProvider.PLAY_BILLING, "tok-prior");
        prior.markPaid("play:orderId=GPA-prior", LocalDateTime.now());
        setField(prior, "id", 99L);
        given(paymentRepository.findByPurchaseToken("tok-prior")).willReturn(Optional.of(prior));
        given(subscriptionRepository.findByPaymentId(99L)).willReturn(Optional.empty());

        var result = service.verifyPlayBilling(1L, "iap_three_day", "tok-prior");

        assertThat(result.paymentId()).isEqualTo("play-prior");
        // Google API 도, 신규 save 도, ack 도 없어야 한다.
        verify(playBillingClient, times(0)).verifyProduct(any(), any());
        verify(paymentRepository, times(0)).save(any(PaymentEntity.class));
        verify(subscriptionRepository, times(0)).save(any(SubscriptionEntity.class));
        verify(playBillingClient, times(0)).acknowledge(any(), any());
    }

    @Test
    @DisplayName("verifyPlayBilling: 다른 memberId 가 같은 PAID purchaseToken 재사용 시 FORBIDDEN — 토큰 도용 차단")
    void verifyPlayBillingRejectsTokenStealingFromDifferentMember() {
        properties.setReviewerNicknames("");
        PaymentEntity prior = new PaymentEntity(
                "play-prior", 99L, null, "문어CBT 3일 이용권",
                SubscriptionPlan.THREE_DAY, 3900, 3900, 0,
                com.sqldpass.persistent.payment.PaymentProvider.PLAY_BILLING, "tok-stolen");
        prior.markPaid("play:orderId=GPA-prior", LocalDateTime.now());
        given(paymentRepository.findByPurchaseToken("tok-stolen")).willReturn(Optional.of(prior));

        assertThatThrownBy(() ->
                service.verifyPlayBilling(1L, "iap_three_day", "tok-stolen"))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);

        verify(playBillingClient, times(0)).verifyProduct(any(), any());
        verify(paymentRepository, times(0)).save(any(PaymentEntity.class));
        verify(subscriptionRepository, times(0)).save(any(SubscriptionEntity.class));
        verify(playBillingClient, times(0)).acknowledge(any(), any());
    }

    @Test
    @DisplayName("verifyPlayBilling: PENDING 상태의 타 회원 토큰 재사용도 FORBIDDEN — PAID 분기 진입 전 차단")
    void verifyPlayBillingRejectsPendingTokenStealing() {
        properties.setReviewerNicknames("");
        PaymentEntity prior = new PaymentEntity(
                "play-prior-pending", 99L, null, "문어CBT 3일 이용권",
                SubscriptionPlan.THREE_DAY, 3900, 3900, 0,
                com.sqldpass.persistent.payment.PaymentProvider.PLAY_BILLING, "tok-pending");
        // markPaid 미호출 — status=PENDING.
        given(paymentRepository.findByPurchaseToken("tok-pending")).willReturn(Optional.of(prior));

        assertThatThrownBy(() ->
                service.verifyPlayBilling(1L, "iap_three_day", "tok-pending"))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);

        verify(playBillingClient, times(0)).verifyProduct(any(), any());
        verify(paymentRepository, times(0)).save(any(PaymentEntity.class));
    }

    @Test
    @DisplayName("verifyPlayBilling: UNLIMITED 활성 회원이 ONE_MONTH 추가 결제 시도 → INVALID_INPUT, 결제/구독/ack 0회")
    void verifyPlayBilling_UNLIMITED_활성에서_재구매_INVALID_INPUT() {
        properties.setReviewerNicknames("");
        given(paymentRepository.findByPurchaseToken("tok-new")).willReturn(Optional.empty());
        var info = new PlayBillingClient.PlayPurchaseInfo(0, 0, System.currentTimeMillis(),
                "GPA-test", "KR");
        given(playBillingClient.verifyProduct("iap_one_month", "tok-new")).willReturn(info);

        SubscriptionEntity active = new SubscriptionEntity(
                1L, SubscriptionPlan.UNLIMITED, 100L, LocalDateTime.now(), null);
        given(subscriptionRepository.findActiveByMemberId(any(), any()))
                .willReturn(java.util.List.of(active));

        assertThatThrownBy(() ->
                service.verifyPlayBilling(1L, "iap_one_month", "tok-new"))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);

        verify(paymentRepository, times(0)).save(any(PaymentEntity.class));
        verify(subscriptionRepository, times(0)).save(any(SubscriptionEntity.class));
        verify(playBillingClient, times(0)).acknowledge(any(), any());
    }

    @Test
    @DisplayName("verifyPlayBilling: 같은 plan(ONE_MONTH) 재결제 → INVALID_INPUT")
    void verifyPlayBilling_같은_plan_재구매_INVALID_INPUT() {
        properties.setReviewerNicknames("");
        given(paymentRepository.findByPurchaseToken("tok-same")).willReturn(Optional.empty());
        var info = new PlayBillingClient.PlayPurchaseInfo(0, 0, System.currentTimeMillis(),
                "GPA-test", "KR");
        given(playBillingClient.verifyProduct("iap_one_month", "tok-same")).willReturn(info);

        SubscriptionEntity active = new SubscriptionEntity(
                1L, SubscriptionPlan.ONE_MONTH, 100L,
                LocalDateTime.now().minusDays(5), LocalDateTime.now().plusDays(25));
        given(subscriptionRepository.findActiveByMemberId(any(), any()))
                .willReturn(java.util.List.of(active));

        assertThatThrownBy(() ->
                service.verifyPlayBilling(1L, "iap_one_month", "tok-same"))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);

        verify(paymentRepository, times(0)).save(any(PaymentEntity.class));
        verify(subscriptionRepository, times(0)).save(any(SubscriptionEntity.class));
        verify(playBillingClient, times(0)).acknowledge(any(), any());
    }

    @Test
    @DisplayName("verifyPlayBilling: ONE_MONTH 활성에서 THREE_DAY 다운그레이드 시도 → INVALID_INPUT")
    void verifyPlayBilling_다운그레이드_INVALID_INPUT() {
        properties.setReviewerNicknames("");
        given(paymentRepository.findByPurchaseToken("tok-down")).willReturn(Optional.empty());
        var info = new PlayBillingClient.PlayPurchaseInfo(0, 0, System.currentTimeMillis(),
                "GPA-test", "KR");
        given(playBillingClient.verifyProduct("iap_three_day", "tok-down")).willReturn(info);

        SubscriptionEntity active = new SubscriptionEntity(
                1L, SubscriptionPlan.ONE_MONTH, 100L,
                LocalDateTime.now().minusDays(5), LocalDateTime.now().plusDays(25));
        given(subscriptionRepository.findActiveByMemberId(any(), any()))
                .willReturn(java.util.List.of(active));

        assertThatThrownBy(() ->
                service.verifyPlayBilling(1L, "iap_three_day", "tok-down"))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);

        verify(paymentRepository, times(0)).save(any(PaymentEntity.class));
        verify(subscriptionRepository, times(0)).save(any(SubscriptionEntity.class));
        verify(playBillingClient, times(0)).acknowledge(any(), any());
    }

    @Test
    @DisplayName("revokePlayBillingByToken: 결제 + 구독 모두 찾아 expiresAt=now 로 강제 만료")
    void revokePlayBillingByTokenExpiresSubscription() {
        PaymentEntity payment = new PaymentEntity(
                "play-x", 1L, null, "문어CBT 3일 이용권",
                SubscriptionPlan.THREE_DAY, 3900, 3900, 0,
                com.sqldpass.persistent.payment.PaymentProvider.PLAY_BILLING, "tok-x");
        payment.markPaid("play:orderId=GPA-x", LocalDateTime.now().minusDays(1));
        setField(payment, "id", 77L);
        given(paymentRepository.findByPurchaseToken("tok-x")).willReturn(Optional.of(payment));

        SubscriptionEntity sub = new SubscriptionEntity(
                1L, SubscriptionPlan.THREE_DAY, 77L,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(2));
        given(subscriptionRepository.findByPaymentId(77L)).willReturn(Optional.of(sub));

        boolean revoked = service.revokePlayBillingByToken("tok-x");

        assertThat(revoked).isTrue();
        assertThat(sub.getExpiresAt()).isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(1));
        verify(historyService, times(1)).record(eq(1L), eq(SubscriptionPlan.THREE_DAY),
                eq(com.sqldpass.persistent.payment.SubscriptionHistoryAction.REFUNDED),
                anyString(), eq(null), eq(77L));
    }

    @Test
    @DisplayName("revokePlayBillingByToken: 매칭되는 결제 없으면 false 반환")
    void revokePlayBillingByTokenNoOpWhenMissing() {
        given(paymentRepository.findByPurchaseToken("tok-missing")).willReturn(Optional.empty());
        assertThat(service.revokePlayBillingByToken("tok-missing")).isFalse();
    }

    private MemberEntity newMember(Long id, String nickname) {
        MemberEntity m = new MemberEntity("google", "g-" + id, nickname);
        setField(m, "id", id);
        return m;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> cls = target.getClass();
            Field f = null;
            while (cls != null && f == null) {
                try {
                    f = cls.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ignored) {
                    cls = cls.getSuperclass();
                }
            }
            if (f == null) throw new NoSuchFieldException(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

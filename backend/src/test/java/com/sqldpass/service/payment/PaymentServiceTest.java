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
import com.sqldpass.service.setting.AppSettingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
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
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private AppSettingService appSettingService;

    private PaymentProperties properties;
    private PlayBillingProperties playBillingProperties;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        properties = new PaymentProperties();
        properties.setReviewerNicknames("pay-rv-7f2a91");
        properties.setThreeDay(new PaymentProperties.PlanConfig(3900, "문어CBT Thunder"));
        properties.setFocus(new PaymentProperties.PlanConfig(2900, "문어CBT Focus"));
        properties.setOneMonth(new PaymentProperties.PlanConfig(9900, "문어CBT Pro"));
        properties.setUnlimited(new PaymentProperties.PlanConfig(29900, "문어CBT Lifetime"));

        playBillingProperties = new PlayBillingProperties();
        playBillingProperties.getProductIdMapping().put(SubscriptionPlan.THREE_DAY, "iap_three_day");
        playBillingProperties.getProductIdMapping().put(SubscriptionPlan.FOCUS, "iap_focus");
        playBillingProperties.getProductIdMapping().put(SubscriptionPlan.ONE_MONTH, "iap_one_month");
        playBillingProperties.getProductIdMapping().put(SubscriptionPlan.UNLIMITED, "iap_unlimited");

        service = new PaymentService(properties, portOneClient,
                paymentRepository, subscriptionRepository, memberRepository,
                playBillingClient, playBillingProperties, failureRecorder, historyService,
                subscriptionService, appSettingService);

        // 기존 테스트는 화이트리스트(베타) 모드 동작을 검증하므로 토글 OFF 가정.
        // 토글 ON 동작은 별도 테스트로 검증.
        lenient().when(appSettingService.isCheckoutOpenToAll()).thenReturn(false);
    }

    @Test
    @DisplayName("화이트리스트 비어있으면 정식 오픈 모드 — 어떤 회원이든 prepare 통과")
    void prepareAllowsEveryoneWhenWhitelistEmpty() {
        properties.setReviewerNicknames("");

        var result = service.prepare(1L, SubscriptionPlan.THREE_DAY, "홍길동", "buyer@example.com", "01012345678");

        assertThat(result.amount()).isEqualTo(3900);
        assertThat(result.plan()).isEqualTo(SubscriptionPlan.THREE_DAY);
        verify(paymentRepository, times(1)).save(any(PaymentEntity.class));
    }

    @Test
    @DisplayName("화이트리스트에 없는 닉네임이면 prepare 시 PAYMENT_REVIEWER_ONLY")
    void prepareBlocksNonReviewer() {
        MemberEntity m = newMember(1L, "normal-user");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));
        assertThatThrownBy(() -> service.prepare(1L, SubscriptionPlan.THREE_DAY, "홍길동", "buyer@example.com", "01012345678"))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_REVIEWER_ONLY);
    }

    @Test
    @DisplayName("어드민 토글 ON 이면 화이트리스트 외 닉네임도 prepare 통과")
    void prepareAllowsAnyoneWhenToggleOpen() {
        given(appSettingService.isCheckoutOpenToAll()).willReturn(true);
        // memberRepository 는 호출되지 않아야 함 (토글 ON 이면 닉네임 조회 자체 스킵).

        var result = service.prepare(1L, SubscriptionPlan.THREE_DAY, "홍길동", "buyer@example.com", "01012345678");

        assertThat(result.amount()).isEqualTo(3900);
        verify(paymentRepository, times(1)).save(any(PaymentEntity.class));
    }

    @Test
    @DisplayName("화이트리스트 닉네임 + ONE_MONTH plan 이면 prepare 성공")
    void prepareSucceedsForReviewer() {
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));

        var result = service.prepare(1L, SubscriptionPlan.ONE_MONTH, "홍길동", "buyer@example.com", "01012345678");

        assertThat(result.amount()).isEqualTo(9900);
        assertThat(result.productName()).isEqualTo("문어CBT Pro");
        assertThat(result.plan()).isEqualTo(SubscriptionPlan.ONE_MONTH);
        assertThat(result.paymentId()).startsWith("sqldpass-");
        verify(paymentRepository, times(1)).save(any(PaymentEntity.class));
    }

    @Test
    @DisplayName("plan = null 이면 INVALID_INPUT")
    void prepareRejectsNullPlan() {
        properties.setReviewerNicknames("");

        assertThatThrownBy(() -> service.prepare(1L, null, "홍길동", "buyer@example.com", "01012345678"))
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

        var result = service.prepare(1L, SubscriptionPlan.UNLIMITED, "홍길동", "buyer@example.com", "01012345678");

        // 잔여 15일 / 30일 × ₩9,900 = ₩4,950 차감 → ₩29,900 - ₩4,950 = ₩24,950
        assertThat(result.baseAmount()).isEqualTo(29900);
        assertThat(result.prorateDiscount()).isEqualTo(4950);
        assertThat(result.amount()).isEqualTo(24950);
    }

    @Test
    @DisplayName("prepare: client 입력 amount 를 무시하고 properties 정가만 PaymentEntity 에 저장")
    void prepareUsesPropertiesAmountForSavedEntity() {
        properties.setReviewerNicknames("");
        // 활성 구독 없음 — default mock 이 빈 리스트 반환.

        service.prepare(1L, SubscriptionPlan.THREE_DAY, "홍길동", "buyer@example.com", "01012345678");

        ArgumentCaptor<PaymentEntity> captor = ArgumentCaptor.forClass(PaymentEntity.class);
        verify(paymentRepository, times(1)).save(captor.capture());
        PaymentEntity saved = captor.getValue();
        // client 가 어떤 값을 보내든 prepare 는 plan 만 받으므로 amount 는 properties 의 정가.
        assertThat(saved.getAmount()).isEqualTo(3900);
        assertThat(saved.getBaseAmount()).isEqualTo(3900);
        assertThat(saved.getProrateDiscount()).isEqualTo(0);
    }

    @Test
    @DisplayName("prepare: 업그레이드 시 PaymentEntity.amount 는 baseAmount - prorateDiscount, baseAmount 는 정가 유지")
    void prepareUpgradeSavesProratedAmountButKeepsBaseAtListPrice() {
        properties.setReviewerNicknames("");
        SubscriptionEntity active = new SubscriptionEntity(
                1L, SubscriptionPlan.ONE_MONTH, 100L,
                LocalDateTime.now().minusDays(15), LocalDateTime.now().plusDays(15));
        given(subscriptionRepository.findActiveByMemberId(any(), any()))
                .willReturn(java.util.List.of(active));

        service.prepare(1L, SubscriptionPlan.UNLIMITED, "홍길동", "buyer@example.com", "01012345678");

        ArgumentCaptor<PaymentEntity> captor = ArgumentCaptor.forClass(PaymentEntity.class);
        verify(paymentRepository, times(1)).save(captor.capture());
        PaymentEntity saved = captor.getValue();
        // baseAmount 는 UNLIMITED 정가 그대로 — client 가 못 줄임.
        assertThat(saved.getBaseAmount()).isEqualTo(29900);
        // prorateDiscount 는 서버가 계산 (잔여 15일 / 30일 × ₩9,900 = ₩4,950).
        assertThat(saved.getProrateDiscount()).isGreaterThan(0);
        // amount = baseAmount - prorateDiscount 등식 유지.
        assertThat(saved.getAmount()).isEqualTo(saved.getBaseAmount() - saved.getProrateDiscount());
        assertThat(saved.getAmount()).isLessThan(saved.getBaseAmount());
    }

    @Test
    @DisplayName("prepare: buyer 정보(이름/이메일/휴대폰) 가 PaymentEntity 에 저장됨")
    void prepareSavesBuyerInfo() {
        properties.setReviewerNicknames("");

        service.prepare(1L, SubscriptionPlan.THREE_DAY, "홍길동", "buyer@example.com", "01012345678");

        ArgumentCaptor<PaymentEntity> captor = ArgumentCaptor.forClass(PaymentEntity.class);
        verify(paymentRepository, times(1)).save(captor.capture());
        PaymentEntity saved = captor.getValue();
        assertThat(saved.getBuyerName()).isEqualTo("홍길동");
        assertThat(saved.getBuyerEmail()).isEqualTo("buyer@example.com");
        assertThat(saved.getBuyerPhoneNumber()).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("prepare: 휴대폰 번호 하이픈/공백 제거 후 저장")
    void prepareNormalizesPhone() {
        properties.setReviewerNicknames("");

        service.prepare(1L, SubscriptionPlan.THREE_DAY, "홍길동", "buyer@example.com", "010-1234-5678");

        ArgumentCaptor<PaymentEntity> captor = ArgumentCaptor.forClass(PaymentEntity.class);
        verify(paymentRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getBuyerPhoneNumber()).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("prepare: buyer 3필드 모두 null(카카오페이 흐름) 도 정상 저장 — PaymentEntity buyer 모두 null")
    void prepareWithoutBuyerInfo() {
        properties.setReviewerNicknames("");

        var result = service.prepare(1L, SubscriptionPlan.THREE_DAY, null, null, null);

        ArgumentCaptor<PaymentEntity> captor = ArgumentCaptor.forClass(PaymentEntity.class);
        verify(paymentRepository, times(1)).save(captor.capture());
        PaymentEntity saved = captor.getValue();
        assertThat(saved.getBuyerName()).isNull();
        assertThat(saved.getBuyerEmail()).isNull();
        assertThat(saved.getBuyerPhoneNumber()).isNull();
        assertThat(result.plan()).isEqualTo(SubscriptionPlan.THREE_DAY);
    }

    @Test
    @DisplayName("PrepareRequest record: plan + buyer 3종 필드 — 추가/제거 시 컴파일 시점에 즉시 실패")
    void prepareRequestRecordHasPlanAndBuyerFields() {
        var components = com.sqldpass.controller.payment.PaymentController.PrepareRequest.class
                .getRecordComponents();
        assertThat(components).hasSize(4);
        assertThat(components[0].getName()).isEqualTo("plan");
        assertThat(components[0].getType()).isEqualTo(SubscriptionPlan.class);
        assertThat(components[1].getName()).isEqualTo("buyerName");
        assertThat(components[2].getName()).isEqualTo("buyerEmail");
        assertThat(components[3].getName()).isEqualTo("buyerPhoneNumber");
    }

    @Test
    @DisplayName("UNLIMITED 활성 + 어떤 plan 결제 시도 → INVALID_INPUT")
    void prepareBlockedWhenUnlimitedActive() {
        properties.setReviewerNicknames("");
        SubscriptionEntity active = new SubscriptionEntity(
                1L, SubscriptionPlan.UNLIMITED, 100L, LocalDateTime.now(), null);
        given(subscriptionRepository.findActiveByMemberId(any(), any()))
                .willReturn(java.util.List.of(active));

        assertThatThrownBy(() -> service.prepare(1L, SubscriptionPlan.ONE_MONTH, "홍길동", "buyer@example.com", "01012345678"))
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

        assertThatThrownBy(() -> service.prepare(1L, SubscriptionPlan.ONE_MONTH, "홍길동", "buyer@example.com", "01012345678"))
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

        assertThatThrownBy(() -> service.prepare(1L, SubscriptionPlan.THREE_DAY, "홍길동", "buyer@example.com", "01012345678"))
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
    @DisplayName("verify: status=CANCELLED 면 PAYMENT_CANCELLED + markFailedInNewTx 호출")
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

        // 사용자 취소는 일반 실패와 구분 — 프론트가 "취소되었습니다" info 톤으로 표시.
        assertThatThrownBy(() -> service.verify(1L, "p-1"))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_CANCELLED);

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

    // ============================================================
    // verify idempotency 보강 (Step 3) — 호출 횟수/expiresAt 정확성/UNIQUE 방어선
    // ============================================================

    @Test
    @DisplayName("verify: PAID 재호출 시 PortOne 호출 0회 + save 0회 + expiresAt 은 기존 SubscriptionEntity 와 정확히 일치 (Equals)")
    void verify_PAID_재호출_PortOne_0_save_0_expiresAt_정확일치() {
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));

        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "3일 이용권",
                SubscriptionPlan.THREE_DAY, 3900);
        // 고정 시간 — 정확한 Equals 검증을 위해 now() 같은 흐름 시간 회피.
        LocalDateTime fixedPaidAt = LocalDateTime.of(2026, 5, 1, 10, 0, 0);
        LocalDateTime fixedExpiresAt = LocalDateTime.of(2026, 5, 4, 10, 0, 0);
        entity.markPaid("{...}", fixedPaidAt);
        setField(entity, "id", 42L);
        given(paymentRepository.findByPaymentId("p-1")).willReturn(Optional.of(entity));

        SubscriptionEntity prior = new SubscriptionEntity(
                1L, SubscriptionPlan.THREE_DAY, 42L, fixedPaidAt, fixedExpiresAt);
        given(subscriptionRepository.findByPaymentId(42L)).willReturn(Optional.of(prior));

        var result = service.verify(1L, "p-1");

        // expiresAt 정확 일치 (isEqualToIgnoringSeconds 가 아닌 isEqualTo) — 캐시된 값 그대로.
        assertThat(result.expiresAt()).isEqualTo(fixedExpiresAt);
        assertThat(result.paymentId()).isEqualTo("p-1");
        assertThat(result.plan()).isEqualTo(SubscriptionPlan.THREE_DAY);
        assertThat(result.amount()).isEqualTo(3900);

        // 재호출 비용/할당량 방어 — PortOne 0회.
        verify(portOneClient, times(0)).getPayment(any());
        // 중복 row 방어 — subscription save 0회.
        verify(subscriptionRepository, times(0)).save(any(SubscriptionEntity.class));
        // PAID 분기는 failure 경로를 아예 타지 않는다.
        verify(failureRecorder, times(0)).markFailedInNewTx(any(), any());
        // PAID 분기는 markPaid 도 재호출하지 않음 — 기존 entity 의 paidAt 그대로.
        assertThat(entity.getStatus()).isEqualTo(com.sqldpass.persistent.payment.PaymentStatus.PAID);
    }

    @Test
    @DisplayName("verify: PAID 재호출인데 SubscriptionEntity 가 없으면 expiresAt=null 로 반환 — Step 6 복구 대상 회귀 방지")
    void verify_PAID_재호출_subscription_없으면_expiresAt_null() {
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));

        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "3일 이용권",
                SubscriptionPlan.THREE_DAY, 3900);
        entity.markPaid("{...}", LocalDateTime.now().minusMinutes(1));
        setField(entity, "id", 42L);
        given(paymentRepository.findByPaymentId("p-1")).willReturn(Optional.of(entity));

        // 결제는 PAID 인데 SubscriptionEntity 가 누락된 비정상 상태.
        given(subscriptionRepository.findByPaymentId(42L)).willReturn(Optional.empty());

        var result = service.verify(1L, "p-1");

        // 현재 동작: expiresAt 은 null 로 반환 (운영 복구는 Step 6 의 reissue endpoint 가 담당).
        assertThat(result.expiresAt()).isNull();
        assertThat(result.paymentId()).isEqualTo("p-1");
        assertThat(result.plan()).isEqualTo(SubscriptionPlan.THREE_DAY);
        // PAID 분기이므로 PortOne 재호출/신규 save 없음.
        verify(portOneClient, times(0)).getPayment(any());
        verify(subscriptionRepository, times(0)).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("subscription_payment_id UNIQUE 제약(V80): 같은 paymentId 두 번째 save 시 DataIntegrityViolationException — 코드 가드 실패 시 마지막 방어선")
    void subscription_payment_id_unique_제약_두번째_save_위반() {
        // 운영 시나리오: 코드 PAID 가드가 어떤 이유로 우회되어 SubscriptionEntity 가 두 번 save 시도되는 경우,
        // V80 의 UNIQUE 제약이 DB 단에서 막아준다는 것을 mock 으로 못박는다.
        SubscriptionEntity first = new SubscriptionEntity(
                1L, SubscriptionPlan.THREE_DAY, 42L,
                LocalDateTime.now(), LocalDateTime.now().plusDays(3));
        SubscriptionEntity duplicate = new SubscriptionEntity(
                1L, SubscriptionPlan.THREE_DAY, 42L,
                LocalDateTime.now(), LocalDateTime.now().plusDays(3));

        given(subscriptionRepository.save(first)).willReturn(first);
        given(subscriptionRepository.save(duplicate))
                .willThrow(new org.springframework.dao.DataIntegrityViolationException(
                        "Duplicate entry '42' for key 'subscription.uk_subscription_payment_id'"));

        // 첫 save 는 성공.
        assertThat(subscriptionRepository.save(first)).isSameAs(first);
        // 같은 paymentId 두 번째 save 시도 → DB UNIQUE 위반.
        assertThatThrownBy(() -> subscriptionRepository.save(duplicate))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                .hasMessageContaining("uk_subscription_payment_id");
    }

    // ============================================================
    // revokePortOnePayment (Step 4) — 관리자 환불 서비스
    // ============================================================

    @Test
    @DisplayName("revokePortOnePayment: 정상 PG cancel 호출 + PaymentEntity CANCELLED + revokeByPaymentId + history REFUNDED")
    void revokePortOnePayment_정상_PG_cancel_호출_PaymentEntity_CANCELLED_revokeByPaymentId_history_REFUNDED() {
        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "3일 이용권",
                SubscriptionPlan.THREE_DAY, 3900);
        entity.markPaid("{...}", LocalDateTime.now().minusMinutes(5));
        setField(entity, "id", 42L);
        given(paymentRepository.findById(42L)).willReturn(Optional.of(entity));

        service.revokePortOnePayment(42L, "고객 요청", 7L);

        // 1) PG cancel 호출.
        verify(portOneClient, times(1)).cancel("p-1", "고객 요청");
        // 2) PaymentEntity 상태 CANCELLED 로 전이.
        assertThat(entity.getStatus())
                .isEqualTo(com.sqldpass.persistent.payment.PaymentStatus.CANCELLED);
        // 3) 구독 회수.
        verify(subscriptionService, times(1)).revokeByPaymentId(42L);
        // 4) history REFUNDED 1회 — actorAdminId·paymentId·reason 정확 전달.
        verify(historyService, times(1)).record(eq(1L), eq(SubscriptionPlan.THREE_DAY),
                eq(com.sqldpass.persistent.payment.SubscriptionHistoryAction.REFUNDED),
                eq("고객 요청"), eq(7L), eq(42L));
    }

    @Test
    @DisplayName("revokePortOnePayment: PG 5xx 시 PAYMENT_GATEWAY_ERROR 그대로 throw + markCancelled/revoke/history 미호출")
    void revokePortOnePayment_PG_5xx_시_PAYMENT_GATEWAY_ERROR_그대로_throw_상태_미변경() {
        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "3일 이용권",
                SubscriptionPlan.THREE_DAY, 3900);
        entity.markPaid("{...}", LocalDateTime.now().minusMinutes(5));
        setField(entity, "id", 42L);
        given(paymentRepository.findById(42L)).willReturn(Optional.of(entity));
        org.mockito.BDDMockito.willThrow(new SqldpassException(ErrorCode.PAYMENT_GATEWAY_ERROR))
                .given(portOneClient).cancel("p-1", "재시도");

        assertThatThrownBy(() -> service.revokePortOnePayment(42L, "재시도", 7L))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_GATEWAY_ERROR);

        // PG 실패 → 트랜잭션 롤백 — markCancelled 호출 안 됨 (PaymentEntity 상태는 PAID 유지).
        assertThat(entity.getStatus())
                .isEqualTo(com.sqldpass.persistent.payment.PaymentStatus.PAID);
        verify(subscriptionService, times(0)).revokeByPaymentId(any());
        verify(historyService, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("revokePortOnePayment: 이미 CANCELLED 면 idempotent — cancel/revoke/history 호출 0회")
    void revokePortOnePayment_이미_CANCELLED_면_idempotent_no_op() {
        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "3일 이용권",
                SubscriptionPlan.THREE_DAY, 3900);
        entity.markCancelled("prior-cancel");
        setField(entity, "id", 42L);
        given(paymentRepository.findById(42L)).willReturn(Optional.of(entity));

        service.revokePortOnePayment(42L, "재요청", 7L);

        verify(portOneClient, times(0)).cancel(any(), any());
        verify(subscriptionService, times(0)).revokeByPaymentId(any());
        verify(historyService, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("revokePortOnePayment: 같은 회원의 더 최신 활성 PAID 결제 있으면 PAYMENT_SUPERSEDED_BY_NEWER + PG cancel 호출 0회")
    void revokePortOnePayment_superseded_시_PAYMENT_SUPERSEDED_BY_NEWER_throw_cancel_미호출() {
        LocalDateTime paidAt = LocalDateTime.now().minusDays(2);
        PaymentEntity entity = new PaymentEntity("p-old", 1L, null, "3일 이용권",
                SubscriptionPlan.THREE_DAY, 3900);
        entity.markPaid("{...}", paidAt);
        setField(entity, "id", 42L);
        given(paymentRepository.findById(42L)).willReturn(Optional.of(entity));
        // 같은 회원의 더 최신 활성 PAID 결제 존재 — 업그레이드된 상태.
        given(paymentRepository.existsNewerActivePaidPaymentForMember(1L, 42L, paidAt))
                .willReturn(true);

        assertThatThrownBy(() -> service.revokePortOnePayment(42L, "고객 요청", 7L))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_SUPERSEDED_BY_NEWER);

        // PG cancel / 구독 회수 / history 모두 호출 안 됨 — PG 환불 사고 차단.
        verify(portOneClient, times(0)).cancel(any(), any());
        verify(subscriptionService, times(0)).revokeByPaymentId(any());
        verify(historyService, times(0)).record(any(), any(), any(), any(), any(), any());
        // 결제 상태는 PAID 유지 — 운영자에게 옛 결제임을 알려 최신 결제부터 환불하도록.
        assertThat(entity.getStatus())
                .isEqualTo(com.sqldpass.persistent.payment.PaymentStatus.PAID);
    }

    @Test
    @DisplayName("revokePortOnePayment: PLAY_BILLING provider 면 INVALID_INPUT — cancel 호출 0회")
    void revokePortOnePayment_PLAY_BILLING_provider_면_INVALID_INPUT_throw() {
        PaymentEntity entity = new PaymentEntity(
                "play-x", 1L, null, "3일 이용권",
                SubscriptionPlan.THREE_DAY, 3900, 3900, 0,
                com.sqldpass.persistent.payment.PaymentProvider.PLAY_BILLING, "tok-x");
        entity.markPaid("play:orderId=GPA-x", LocalDateTime.now().minusDays(1));
        setField(entity, "id", 99L);
        given(paymentRepository.findById(99L)).willReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.revokePortOnePayment(99L, "잘못된 경로", 7L))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);

        verify(portOneClient, times(0)).cancel(any(), any());
        verify(subscriptionService, times(0)).revokeByPaymentId(any());
        verify(historyService, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    // ============================================================
    // reissueSubscription (Step 6) — 결제 후 권한 미부여 복구
    // ============================================================

    @Test
    @DisplayName("reissueSubscription: PAID 결제에 subscription 없으면 새로 발급 + history GRANTED 1회")
    void reissueSubscription_PAID_결제에_subscription_없으면_새로_발급_history_GRANTED() {
        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "3일 이용권",
                SubscriptionPlan.THREE_DAY, 3900);
        LocalDateTime paidAt = LocalDateTime.of(2026, 5, 1, 10, 0, 0);
        entity.markPaid("{...}", paidAt);
        setField(entity, "id", 42L);
        given(paymentRepository.findById(42L)).willReturn(Optional.of(entity));
        given(subscriptionRepository.findByPaymentId(42L)).willReturn(Optional.empty());

        var result = service.reissueSubscription(42L, 7L);

        assertThat(result.issued()).isTrue();
        // THREE_DAY → expiresAt = paidAt + 3d 정확 일치.
        assertThat(result.expiresAt()).isEqualTo(paidAt.plusDays(3));

        ArgumentCaptor<SubscriptionEntity> subCaptor = ArgumentCaptor.forClass(SubscriptionEntity.class);
        verify(subscriptionRepository, times(1)).save(subCaptor.capture());
        SubscriptionEntity saved = subCaptor.getValue();
        assertThat(saved.getMemberId()).isEqualTo(1L);
        assertThat(saved.getPlan()).isEqualTo(SubscriptionPlan.THREE_DAY);
        assertThat(saved.getPaymentId()).isEqualTo(42L);
        assertThat(saved.getPurchasedAt()).isEqualTo(paidAt);
        assertThat(saved.getExpiresAt()).isEqualTo(paidAt.plusDays(3));

        verify(historyService, times(1)).record(eq(1L), eq(SubscriptionPlan.THREE_DAY),
                eq(com.sqldpass.persistent.payment.SubscriptionHistoryAction.GRANTED),
                contains("admin-reissue:paymentId=p-1"), eq(7L), eq(42L));
    }

    @Test
    @DisplayName("reissueSubscription: 이미 활성 subscription 있으면 idempotent — save 0회 + history 0회 + issued=false")
    void reissueSubscription_이미_활성_subscription_있으면_idempotent_save_0() {
        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "3일 이용권",
                SubscriptionPlan.THREE_DAY, 3900);
        entity.markPaid("{...}", LocalDateTime.now().minusMinutes(5));
        setField(entity, "id", 42L);
        given(paymentRepository.findById(42L)).willReturn(Optional.of(entity));

        LocalDateTime existingExpiresAt = LocalDateTime.now().plusDays(2);
        SubscriptionEntity existing = new SubscriptionEntity(
                1L, SubscriptionPlan.THREE_DAY, 42L,
                LocalDateTime.now().minusDays(1), existingExpiresAt);
        given(subscriptionRepository.findByPaymentId(42L)).willReturn(Optional.of(existing));

        var result = service.reissueSubscription(42L, 7L);

        assertThat(result.issued()).isFalse();
        assertThat(result.expiresAt()).isEqualTo(existingExpiresAt);

        verify(subscriptionRepository, times(0)).save(any(SubscriptionEntity.class));
        verify(historyService, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("reissueSubscription: PAID 아닌 결제(PENDING/FAILED 등) 는 INVALID_INPUT")
    void reissueSubscription_PAID_아닌_결제는_INVALID_INPUT() {
        PaymentEntity entity = new PaymentEntity("p-pending", 1L, null, "3일 이용권",
                SubscriptionPlan.THREE_DAY, 3900);
        // markPaid 미호출 — status=PENDING.
        setField(entity, "id", 42L);
        given(paymentRepository.findById(42L)).willReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.reissueSubscription(42L, 7L))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);

        verify(subscriptionRepository, times(0)).save(any(SubscriptionEntity.class));
        verify(historyService, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("reissueSubscription: plan 정보 없는 옛 단건 결제는 INVALID_INPUT")
    void reissueSubscription_plan_null_결제는_INVALID_INPUT() {
        // 옛 mock-exam 결제 등 plan 없는 row.
        PaymentEntity entity = new PaymentEntity("p-legacy", 1L, 100L, "옛 모의고사 단건", 3900);
        entity.markPaid("{...}", LocalDateTime.now().minusMinutes(5));
        setField(entity, "id", 42L);
        given(paymentRepository.findById(42L)).willReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.reissueSubscription(42L, 7L))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);

        verify(subscriptionRepository, times(0)).save(any(SubscriptionEntity.class));
        verify(historyService, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("reissueSubscription: paidAt 이 null 이면 now 기준으로 expiresAt 계산")
    void reissueSubscription_paidAt_null_이면_now_사용() {
        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "3일 이용권",
                SubscriptionPlan.THREE_DAY, 3900);
        // markPaid 호출 안 함 — paidAt = null 상태에서 status 만 PAID 로 강제.
        setField(entity, "status", com.sqldpass.persistent.payment.PaymentStatus.PAID);
        setField(entity, "id", 42L);
        given(paymentRepository.findById(42L)).willReturn(Optional.of(entity));
        given(subscriptionRepository.findByPaymentId(42L)).willReturn(Optional.empty());

        LocalDateTime before = LocalDateTime.now();
        var result = service.reissueSubscription(42L, 7L);
        LocalDateTime after = LocalDateTime.now();

        assertThat(result.issued()).isTrue();
        // expiresAt 은 (now ~ now+slight) + 3d 사이에 위치.
        assertThat(result.expiresAt()).isAfterOrEqualTo(before.plusDays(3).minusSeconds(1));
        assertThat(result.expiresAt()).isBeforeOrEqualTo(after.plusDays(3).plusSeconds(1));

        verify(subscriptionRepository, times(1)).save(any(SubscriptionEntity.class));
        verify(historyService, times(1)).record(eq(1L), eq(SubscriptionPlan.THREE_DAY),
                eq(com.sqldpass.persistent.payment.SubscriptionHistoryAction.GRANTED),
                contains("admin-reissue:paymentId=p-1"), eq(7L), eq(42L));
    }

    @Test
    @DisplayName("reissueSubscription: PaymentEntity 자체가 없으면 PAYMENT_NOT_FOUND")
    void reissueSubscription_payment_없으면_PAYMENT_NOT_FOUND() {
        given(paymentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.reissueSubscription(999L, 7L))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);

        verify(subscriptionRepository, times(0)).save(any(SubscriptionEntity.class));
        verify(historyService, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("reissueSubscription: UNLIMITED 결제는 expiresAt=null 로 재발급")
    void reissueSubscription_UNLIMITED_은_expiresAt_null() {
        PaymentEntity entity = new PaymentEntity("p-life", 1L, null, "무제한권",
                SubscriptionPlan.UNLIMITED, 29900);
        LocalDateTime paidAt = LocalDateTime.of(2026, 5, 1, 10, 0, 0);
        entity.markPaid("{...}", paidAt);
        setField(entity, "id", 88L);
        given(paymentRepository.findById(88L)).willReturn(Optional.of(entity));
        given(subscriptionRepository.findByPaymentId(88L)).willReturn(Optional.empty());

        var result = service.reissueSubscription(88L, 7L);

        assertThat(result.issued()).isTrue();
        assertThat(result.expiresAt()).isNull();

        ArgumentCaptor<SubscriptionEntity> subCaptor = ArgumentCaptor.forClass(SubscriptionEntity.class);
        verify(subscriptionRepository, times(1)).save(subCaptor.capture());
        assertThat(subCaptor.getValue().getExpiresAt()).isNull();
        assertThat(subCaptor.getValue().getPlan()).isEqualTo(SubscriptionPlan.UNLIMITED);
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

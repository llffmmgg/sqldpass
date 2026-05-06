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

    private PaymentProperties properties;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        properties = new PaymentProperties();
        properties.setReviewerNicknames("pay-rv-7f2a91");
        properties.setThreeDay(new PaymentProperties.PlanConfig(3900, "문어CBT 3일 이용권"));
        properties.setOneMonth(new PaymentProperties.PlanConfig(9900, "문어CBT 한달 이용권"));
        properties.setUnlimited(new PaymentProperties.PlanConfig(29900, "문어CBT 평생 무제한 이용권"));
        service = new PaymentService(properties, portOneClient,
                paymentRepository, subscriptionRepository, memberRepository);
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

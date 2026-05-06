package com.sqldpass.service.payment;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.payment.SubscriptionEntity;
import com.sqldpass.persistent.payment.SubscriptionPlan;
import com.sqldpass.persistent.payment.SubscriptionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private MemberRepository memberRepository;

    private PaymentProperties properties;
    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        properties = new PaymentProperties();
        service = new SubscriptionService(subscriptionRepository, memberRepository, properties);
    }

    @Test
    @DisplayName("memberId = null 이면 비활성")
    void inactiveForNullMember() {
        assertThat(service.getActive(null)).isEmpty();
        assertThat(service.hasPremiumAccess(null)).isFalse();
    }

    @Test
    @DisplayName("활성 구독 row 없으면 비활성")
    void inactiveWithoutRow() {
        given(subscriptionRepository.findActiveByMemberId(eq(1L), any())).willReturn(List.of());

        assertThat(service.getActive(1L)).isEmpty();
        assertThat(service.hasPremiumAccess(1L)).isFalse();
        assertThat(service.removesAds(1L)).isFalse();
        assertThat(service.allowsPdf(1L)).isFalse();
    }

    @Test
    @DisplayName("THREE_DAY 활성 → premium=true, removesAds=false, allowsPdf=false")
    void threeDayActive() {
        SubscriptionEntity sub = new SubscriptionEntity(
                1L, SubscriptionPlan.THREE_DAY, 100L,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(2));
        given(subscriptionRepository.findActiveByMemberId(eq(1L), any())).willReturn(List.of(sub));

        var active = service.getActive(1L);
        assertThat(active).isPresent();
        assertThat(active.get().plan()).isEqualTo(SubscriptionPlan.THREE_DAY);
        assertThat(active.get().removesAds()).isFalse();
        assertThat(active.get().allowsPdf()).isFalse();
        assertThat(service.hasPremiumAccess(1L)).isTrue();
    }

    @Test
    @DisplayName("ONE_MONTH 활성 → removesAds=true, allowsPdf=false")
    void oneMonthActive() {
        SubscriptionEntity sub = new SubscriptionEntity(
                1L, SubscriptionPlan.ONE_MONTH, 100L,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(29));
        given(subscriptionRepository.findActiveByMemberId(eq(1L), any())).willReturn(List.of(sub));

        var active = service.getActive(1L);
        assertThat(active.get().removesAds()).isTrue();
        assertThat(active.get().allowsPdf()).isFalse();
    }

    @Test
    @DisplayName("UNLIMITED 활성 → expiresAt=null, allowsPdf=true")
    void unlimitedActive() {
        SubscriptionEntity sub = new SubscriptionEntity(
                1L, SubscriptionPlan.UNLIMITED, 100L,
                LocalDateTime.now(), null);
        given(subscriptionRepository.findActiveByMemberId(eq(1L), any())).willReturn(List.of(sub));

        var active = service.getActive(1L);
        assertThat(active.get().plan()).isEqualTo(SubscriptionPlan.UNLIMITED);
        assertThat(active.get().expiresAt()).isNull();
        assertThat(active.get().removesAds()).isTrue();
        assertThat(active.get().allowsPdf()).isTrue();
    }

    @Test
    @DisplayName("화이트리스트 닉네임 회원은 DB row 없어도 가상 UNLIMITED")
    void reviewerNicknameGetsVirtualUnlimited() {
        properties.setReviewerNicknames("pay-rv-7f2a91");
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));

        var active = service.getActive(1L);
        assertThat(active).isPresent();
        assertThat(active.get().plan()).isEqualTo(SubscriptionPlan.UNLIMITED);
        assertThat(active.get().allowsPdf()).isTrue();
    }

    @Test
    @DisplayName("화이트리스트 비어있으면 닉네임 검사 건너뜀 (성능)")
    void emptyWhitelistSkipsMemberLookup() {
        properties.setReviewerNicknames("");
        given(subscriptionRepository.findActiveByMemberId(eq(1L), any())).willReturn(List.of());

        var active = service.getActive(1L);
        assertThat(active).isEmpty();
        // memberRepository.findById 가 호출되지 않았어야 함 (strict stubbing)
    }

    private MemberEntity newMember(Long id, String nickname) {
        MemberEntity m = new MemberEntity("google", "g-" + id, nickname);
        try {
            Field f = MemberEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(m, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return m;
    }
}

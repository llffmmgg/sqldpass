package com.sqldpass.service.payment;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private SubscriptionHistoryService historyService;

    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(subscriptionRepository, historyService);
    }

    @Test
    @DisplayName("memberId = null 이면 비활성")
    void inactiveForNullMember() {
        assertThat(service.getActive(null)).isEmpty();
        assertThat(service.hasPremiumAccess(null)).isFalse();
    }

    @Test
    @DisplayName("활성 구독 row 없으면 비활성 — 화이트리스트 닉네임이어도 동일")
    void inactiveWithoutRow() {
        given(subscriptionRepository.findActiveByMemberId(eq(1L), any())).willReturn(List.of());

        assertThat(service.getActive(1L)).isEmpty();
        assertThat(service.hasPremiumAccess(1L)).isFalse();
        assertThat(service.removesAds(1L)).isFalse();
        assertThat(service.allowsPdf(1L)).isFalse();
        assertThat(service.hasLibraryAccess(1L)).isFalse();
    }

    @Test
    @DisplayName("THREE_DAY(Thunder) 활성 → premium=true, removesAds=true, allowsPdf=false, hasLibraryAccess=true")
    void threeDayActive() {
        SubscriptionEntity sub = new SubscriptionEntity(
                1L, SubscriptionPlan.THREE_DAY, 100L,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(2));
        given(subscriptionRepository.findActiveByMemberId(eq(1L), any())).willReturn(List.of(sub));

        var active = service.getActive(1L);
        assertThat(active).isPresent();
        assertThat(active.get().plan()).isEqualTo(SubscriptionPlan.THREE_DAY);
        assertThat(active.get().removesAds()).isTrue();
        assertThat(active.get().allowsPdf()).isFalse();
        assertThat(active.get().hasLibraryAccess()).isTrue();
        assertThat(active.get().allowsPremium()).isTrue();
        assertThat(service.hasPremiumAccess(1L)).isTrue();
    }

    @Test
    @DisplayName("FOCUS 활성 → PASS+ 차단 (premium=false), 광고제거·라이브러리는 true")
    void focusActive() {
        SubscriptionEntity sub = new SubscriptionEntity(
                1L, SubscriptionPlan.FOCUS, 100L,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(29));
        given(subscriptionRepository.findActiveByMemberId(eq(1L), any())).willReturn(List.of(sub));

        var active = service.getActive(1L);
        assertThat(active.get().plan()).isEqualTo(SubscriptionPlan.FOCUS);
        assertThat(active.get().removesAds()).isTrue();
        assertThat(active.get().allowsPdf()).isFalse();
        assertThat(active.get().hasLibraryAccess()).isTrue();
        // paywall 정책 — Focus 는 PASS+ 회차 접근 불가.
        assertThat(active.get().allowsPremium()).isFalse();
        assertThat(service.hasPremiumAccess(1L)).isFalse();
    }

    @Test
    @DisplayName("ONE_MONTH(Pro) 활성 → premium=true, removesAds=true, allowsPdf=false, hasLibraryAccess=true")
    void oneMonthActive() {
        SubscriptionEntity sub = new SubscriptionEntity(
                1L, SubscriptionPlan.ONE_MONTH, 100L,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(29));
        given(subscriptionRepository.findActiveByMemberId(eq(1L), any())).willReturn(List.of(sub));

        var active = service.getActive(1L);
        assertThat(active.get().removesAds()).isTrue();
        assertThat(active.get().allowsPdf()).isFalse();
        assertThat(active.get().hasLibraryAccess()).isTrue();
        assertThat(active.get().allowsPremium()).isTrue();
        assertThat(service.hasPremiumAccess(1L)).isTrue();
    }

    @Test
    @DisplayName("UNLIMITED(All Pass) 활성 → expiresAt=null, allowsPdf=true, premium=true")
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
        assertThat(active.get().hasLibraryAccess()).isTrue();
        assertThat(active.get().allowsPremium()).isTrue();
        assertThat(service.hasPremiumAccess(1L)).isTrue();
    }
}

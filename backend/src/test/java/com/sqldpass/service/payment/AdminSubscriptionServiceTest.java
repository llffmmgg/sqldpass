package com.sqldpass.service.payment;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
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
import com.sqldpass.persistent.payment.SubscriptionHistoryAction;
import com.sqldpass.persistent.payment.SubscriptionPlan;
import com.sqldpass.persistent.payment.SubscriptionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminSubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private SubscriptionHistoryService historyService;

    private AdminSubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new AdminSubscriptionService(
                subscriptionRepository, memberRepository, historyService);
    }

    @Test
    @DisplayName("grantManual: SubscriptionEntity 저장 + history GRANTED 기록 (actorAdminId 전달)")
    void grantManual_GRANTED_history_기록() {
        MemberEntity member = newMember(7L, "user-7");
        given(memberRepository.findById(7L)).willReturn(Optional.of(member));
        given(subscriptionRepository.save(any(SubscriptionEntity.class)))
                .willAnswer(inv -> inv.getArgument(0));

        var row = service.grantManual(7L, SubscriptionPlan.ONE_MONTH, "보상-9999", 42L);

        assertThat(row.memberId()).isEqualTo(7L);
        assertThat(row.plan()).isEqualTo(SubscriptionPlan.ONE_MONTH);

        verify(subscriptionRepository, times(1)).save(any(SubscriptionEntity.class));
        verify(historyService, times(1)).record(
                eq(7L), eq(SubscriptionPlan.ONE_MONTH),
                eq(SubscriptionHistoryAction.GRANTED), eq("보상-9999"),
                eq(42L), eq(null));
    }

    @Test
    @DisplayName("expireManual: delete 호출 0회, sub.revoke(now) + history EXPIRED 기록")
    void expireManual_revoke_now_호출_및_EXPIRED_history_기록() {
        SubscriptionEntity sub = new SubscriptionEntity(
                7L, SubscriptionPlan.ONE_MONTH, 100L,
                LocalDateTime.now().minusDays(5), LocalDateTime.now().plusDays(25));
        setField(sub, "id", 33L);
        given(subscriptionRepository.findById(33L)).willReturn(Optional.of(sub));

        service.expireManual(33L, "환불 처리", 42L);

        // delete 호출되지 않음 (audit 보존을 위해 row 유지).
        verify(subscriptionRepository, never()).delete(any());

        // expiresAt 가 now 근처로 갱신.
        LocalDateTime now = LocalDateTime.now();
        assertThat(sub.getExpiresAt()).isBetween(now.minusSeconds(2), now.plusSeconds(2));

        verify(historyService, times(1)).record(
                eq(7L), eq(SubscriptionPlan.ONE_MONTH),
                eq(SubscriptionHistoryAction.EXPIRED), anyString(),
                eq(42L), eq(100L));
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

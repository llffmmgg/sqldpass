package com.sqldpass.service.usage;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sqldpass.persistent.payment.SubscriptionPlan;
import com.sqldpass.persistent.usage.DailyUsageEntity;
import com.sqldpass.persistent.usage.DailyUsageRepository;
import com.sqldpass.service.payment.SubscriptionService;
import com.sqldpass.service.payment.SubscriptionService.ActiveSubscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DailyUsageServiceTest {

    private static final Long MEMBER_ID = 42L;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Mock
    private DailyUsageRepository repository;
    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private DailyUsageService service;

    @BeforeEach
    void setUp() {
        // default: 비구독자. memberId=null 등 분기에서는 호출되지 않아 lenient 로.
        lenient().when(subscriptionService.getActive(any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("consumeQuestion: 20문제 1회 호출 → 카운트 20, 예외 없음")
    void consumeQuestion_under_limit() {
        LocalDate today = LocalDate.now(KST);
        given(repository.findByMemberIdAndUsageDate(eq(MEMBER_ID), eq(today)))
                .willReturn(Optional.of(rowWith(MEMBER_ID, today, 20, 0)));

        service.consumeQuestion(MEMBER_ID, 20);

        verify(repository).upsertAndAdd(MEMBER_ID, today, 20, 0);
    }

    @Test
    @DisplayName("consumeQuestion: 누적이 한도 초과 시 QuotaExceededException + 코드 DAILY_QUESTION_LIMIT")
    void consumeQuestion_over_limit_throws() {
        LocalDate today = LocalDate.now(KST);
        // 20 + 20 = 40 → 30 한도 초과
        given(repository.findByMemberIdAndUsageDate(eq(MEMBER_ID), eq(today)))
                .willReturn(Optional.of(rowWith(MEMBER_ID, today, 40, 0)));

        assertThatThrownBy(() -> service.consumeQuestion(MEMBER_ID, 20))
                .isInstanceOf(QuotaExceededException.class)
                .extracting("code", "used", "limit")
                .containsExactly("DAILY_QUESTION_LIMIT", DailyUsageService.DAILY_QUESTION_LIMIT, DailyUsageService.DAILY_QUESTION_LIMIT);
    }

    @Test
    @DisplayName("consumeQuestion: 단일 호출이 한도 초과해도 예외 (예: 31)")
    void consumeQuestion_single_over_limit() {
        LocalDate today = LocalDate.now(KST);
        given(repository.findByMemberIdAndUsageDate(eq(MEMBER_ID), eq(today)))
                .willReturn(Optional.of(rowWith(MEMBER_ID, today, 31, 0)));

        assertThatThrownBy(() -> service.consumeQuestion(MEMBER_ID, 31))
                .isInstanceOf(QuotaExceededException.class);
    }

    @Test
    @DisplayName("consumeQuestion: 활성 구독자(Focus 포함)는 면제 — upsert 호출 안 함")
    void consumeQuestion_active_subscription_skips() {
        given(subscriptionService.getActive(eq(MEMBER_ID)))
                .willReturn(Optional.of(new ActiveSubscription(
                        SubscriptionPlan.FOCUS, LocalDateTime.now().plusDays(10),
                        true, false, true, false))); // allowsPremium=false (Focus) 임에도 면제

        service.consumeQuestion(MEMBER_ID, 100);

        verify(repository, never()).upsertAndAdd(any(), any(), anyInt(), anyInt());
        verify(repository, never()).findByMemberIdAndUsageDate(any(), any());
    }

    @Test
    @DisplayName("consumeQuestion: memberId null 이면 no-op")
    void consumeQuestion_null_member_noop() {
        service.consumeQuestion(null, 10);
        verify(repository, never()).upsertAndAdd(any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("consumeQuestion: delta <= 0 이면 no-op")
    void consumeQuestion_zero_or_negative_delta_noop() {
        service.consumeQuestion(MEMBER_ID, 0);
        service.consumeQuestion(MEMBER_ID, -5);
        verify(repository, never()).upsertAndAdd(any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("consumeMockSession: 1회 호출은 통과")
    void consumeMockSession_first_call_ok() {
        LocalDate today = LocalDate.now(KST);
        given(repository.findByMemberIdAndUsageDate(eq(MEMBER_ID), eq(today)))
                .willReturn(Optional.of(rowWith(MEMBER_ID, today, 0, 1)));

        service.consumeMockSession(MEMBER_ID);

        verify(repository).upsertAndAdd(MEMBER_ID, today, 0, 1);
    }

    @Test
    @DisplayName("consumeMockSession: 2회째에 DAILY_MOCK_LIMIT 예외")
    void consumeMockSession_second_call_throws() {
        LocalDate today = LocalDate.now(KST);
        given(repository.findByMemberIdAndUsageDate(eq(MEMBER_ID), eq(today)))
                .willReturn(Optional.of(rowWith(MEMBER_ID, today, 0, 2)));

        assertThatThrownBy(() -> service.consumeMockSession(MEMBER_ID))
                .isInstanceOf(QuotaExceededException.class)
                .extracting("code", "used", "limit")
                .containsExactly("DAILY_MOCK_LIMIT", DailyUsageService.DAILY_MOCK_SESSION_LIMIT, DailyUsageService.DAILY_MOCK_SESSION_LIMIT);
    }

    @Test
    @DisplayName("consumeMockSession: 구독자 면제 — upsert 호출 안 함")
    void consumeMockSession_active_subscription_skips() {
        given(subscriptionService.getActive(eq(MEMBER_ID)))
                .willReturn(Optional.of(new ActiveSubscription(
                        SubscriptionPlan.ONE_MONTH, LocalDateTime.now().plusDays(20),
                        true, false, true, true)));

        service.consumeMockSession(MEMBER_ID);

        verify(repository, never()).upsertAndAdd(any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("getQuota: 무료 회원 — 사용량 반영, limit 채움")
    void getQuota_free_member() {
        LocalDate today = LocalDate.now(KST);
        given(repository.findByMemberIdAndUsageDate(eq(MEMBER_ID), eq(today)))
                .willReturn(Optional.of(rowWith(MEMBER_ID, today, 18, 0)));

        DailyUsageService.Quota quota = service.getQuota(MEMBER_ID);

        assertThat(quota.questionUsed()).isEqualTo(18);
        assertThat(quota.questionLimit()).isEqualTo(DailyUsageService.DAILY_QUESTION_LIMIT);
        assertThat(quota.mockUsed()).isEqualTo(0);
        assertThat(quota.mockLimit()).isEqualTo(DailyUsageService.DAILY_MOCK_SESSION_LIMIT);
        assertThat(quota.resetAt()).isEqualTo(today.plusDays(1).atStartOfDay());
    }

    @Test
    @DisplayName("getQuota: row 없으면 0/limit")
    void getQuota_free_member_no_row() {
        LocalDate today = LocalDate.now(KST);
        given(repository.findByMemberIdAndUsageDate(eq(MEMBER_ID), eq(today)))
                .willReturn(Optional.empty());

        DailyUsageService.Quota quota = service.getQuota(MEMBER_ID);

        assertThat(quota.questionUsed()).isEqualTo(0);
        assertThat(quota.questionLimit()).isEqualTo(DailyUsageService.DAILY_QUESTION_LIMIT);
    }

    @Test
    @DisplayName("getQuota: 활성 구독자는 limit=null(무제한)")
    void getQuota_subscriber_unlimited() {
        given(subscriptionService.getActive(eq(MEMBER_ID)))
                .willReturn(Optional.of(new ActiveSubscription(
                        SubscriptionPlan.UNLIMITED, null, true, true, true, true)));

        DailyUsageService.Quota quota = service.getQuota(MEMBER_ID);

        assertThat(quota.questionLimit()).isNull();
        assertThat(quota.mockLimit()).isNull();
        assertThat(quota.questionUsed()).isEqualTo(0);
        assertThat(quota.mockUsed()).isEqualTo(0);
        verify(repository, never()).findByMemberIdAndUsageDate(any(), any());
    }

    @Test
    @DisplayName("getQuota: memberId null 도 무제한 응답 — 비로그인은 별도 IP 쿼터 사용")
    void getQuota_null_member_unlimited() {
        DailyUsageService.Quota quota = service.getQuota(null);
        assertThat(quota.questionLimit()).isNull();
        assertThat(quota.mockLimit()).isNull();
    }

    @Test
    @DisplayName("단일 스레드 반복 30번: 정확히 30번째까지 통과, 31번째 호출에 예외")
    void sequential_30_then_31_throws() {
        LocalDate today = LocalDate.now(KST);
        // 호출 횟수에 따라 누적 카운트가 1, 2, ..., 31 로 증가하도록 stub
        final int[] counter = {0};
        given(repository.findByMemberIdAndUsageDate(eq(MEMBER_ID), eq(today)))
                .willAnswer(invocation -> {
                    counter[0] += 1;
                    return Optional.of(rowWith(MEMBER_ID, today, counter[0], 0));
                });

        // 1..30 → 통과
        for (int i = 1; i <= 30; i++) {
            service.consumeQuestion(MEMBER_ID, 1);
        }
        // 31 → 예외
        assertThatThrownBy(() -> service.consumeQuestion(MEMBER_ID, 1))
                .isInstanceOf(QuotaExceededException.class);

        verify(repository, times(31)).upsertAndAdd(eq(MEMBER_ID), eq(today), eq(1), eq(0));
    }

    // ---- helpers ----

    /**
     * 테스트용 DailyUsageEntity — 생성자는 (memberId, usageDate) 만 받고 count 는 0/0 으로 시작하므로
     * 리플렉션으로 questionCount/mockSessionCount 를 주입한다.
     */
    private DailyUsageEntity rowWith(Long memberId, LocalDate usageDate, int questionCount, int mockSessionCount) {
        DailyUsageEntity entity = new DailyUsageEntity(memberId, usageDate);
        try {
            Field qf = DailyUsageEntity.class.getDeclaredField("questionCount");
            qf.setAccessible(true);
            qf.setInt(entity, questionCount);
            Field mf = DailyUsageEntity.class.getDeclaredField("mockSessionCount");
            mf.setAccessible(true);
            mf.setInt(entity, mockSessionCount);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return entity;
    }
}

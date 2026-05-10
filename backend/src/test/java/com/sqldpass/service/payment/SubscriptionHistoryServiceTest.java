package com.sqldpass.service.payment;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sqldpass.persistent.payment.SubscriptionHistoryAction;
import com.sqldpass.persistent.payment.SubscriptionHistoryEntity;
import com.sqldpass.persistent.payment.SubscriptionHistoryRepository;
import com.sqldpass.persistent.payment.SubscriptionPlan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubscriptionHistoryServiceTest {

    @Mock
    private SubscriptionHistoryRepository repository;

    private SubscriptionHistoryService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionHistoryService(repository);
    }

    @Test
    @DisplayName("record 호출 시 entity 가 저장된다 — 필드 매핑 검증")
    void record_호출시_entity_가_저장된다() {
        service.record(42L, SubscriptionPlan.ONE_MONTH, SubscriptionHistoryAction.REFUNDED,
                "RTDN refund", null, 100L);

        ArgumentCaptor<SubscriptionHistoryEntity> captor = ArgumentCaptor.forClass(SubscriptionHistoryEntity.class);
        verify(repository, times(1)).save(captor.capture());

        SubscriptionHistoryEntity saved = captor.getValue();
        assertThat(saved.getMemberId()).isEqualTo(42L);
        assertThat(saved.getPlan()).isEqualTo(SubscriptionPlan.ONE_MONTH);
        assertThat(saved.getAction()).isEqualTo(SubscriptionHistoryAction.REFUNDED);
        assertThat(saved.getReason()).isEqualTo("RTDN refund");
        assertThat(saved.getActorAdminId()).isNull();
        assertThat(saved.getPaymentId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("record 의 occurredAt 은 now 근사값 — 1분 이내")
    void record_의_occurredAt_은_now_근사값() {
        LocalDateTime before = LocalDateTime.now();
        service.record(7L, SubscriptionPlan.UNLIMITED, SubscriptionHistoryAction.GRANTED,
                "manual grant", 99L, null);
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<SubscriptionHistoryEntity> captor = ArgumentCaptor.forClass(SubscriptionHistoryEntity.class);
        verify(repository).save(captor.capture());

        LocalDateTime occurredAt = captor.getValue().getOccurredAt();
        assertThat(occurredAt).isNotNull();
        assertThat(Math.abs(ChronoUnit.SECONDS.between(occurredAt, before))).isLessThan(60);
        assertThat(Math.abs(ChronoUnit.SECONDS.between(occurredAt, after))).isLessThan(60);
        assertThat(captor.getValue().getActorAdminId()).isEqualTo(99L);
        assertThat(captor.getValue().getPaymentId()).isNull();
    }
}

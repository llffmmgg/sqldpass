package com.sqldpass.service.member;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.bookmark.BookmarkEntity;
import com.sqldpass.persistent.bookmark.BookmarkRepository;
import com.sqldpass.persistent.feedback.FeedbackEntity;
import com.sqldpass.persistent.feedback.FeedbackRepository;
import com.sqldpass.persistent.feedback.FeedbackType;
import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.notification.NotificationRepository;
import com.sqldpass.persistent.payment.MockExamPurchaseEntity;
import com.sqldpass.persistent.payment.MockExamPurchaseRepository;
import com.sqldpass.persistent.payment.PaymentEntity;
import com.sqldpass.persistent.payment.PaymentRepository;
import com.sqldpass.persistent.payment.SubscriptionEntity;
import com.sqldpass.persistent.payment.SubscriptionHistoryAction;
import com.sqldpass.persistent.payment.SubscriptionHistoryEntity;
import com.sqldpass.persistent.payment.SubscriptionHistoryRepository;
import com.sqldpass.persistent.payment.SubscriptionPlan;
import com.sqldpass.persistent.payment.SubscriptionRepository;
import com.sqldpass.persistent.solve.SolveEntity;
import com.sqldpass.persistent.solve.SolveRepository;
import com.sqldpass.persistent.usage.DailyUsageEntity;
import com.sqldpass.persistent.usage.DailyUsageRepository;
import com.sqldpass.service.notification.NotificationService;

import jakarta.persistence.EntityManager;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Transactional
class MemberServiceWithdrawTest {

    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private FeedbackRepository feedbackRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private SolveRepository solveRepository;
    @Autowired private BookmarkRepository bookmarkRepository;
    @Autowired private DailyUsageRepository dailyUsageRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private MockExamPurchaseRepository mockExamPurchaseRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private SubscriptionHistoryRepository subscriptionHistoryRepository;
    @Autowired private EntityManager em;

    @Test
    @DisplayName("withdraw deletes personal data and keeps payment records with null memberId")
    void withdraw_endToEnd() {
        // given
        MemberEntity member = memberRepository.save(
                new MemberEntity("google", "providerXYZ", "withdraw-test"));
        Long memberId = member.getId();

        feedbackRepository.save(new FeedbackEntity(FeedbackType.OTHER, memberId, null, "keep me", null));
        notificationService.notify(memberId, "T", "n1", null, null, null);
        notificationService.notify(memberId, "T", "n2", null, null, null);
        bookmarkRepository.save(new BookmarkEntity(memberId, 10L));
        dailyUsageRepository.save(new DailyUsageEntity(memberId, LocalDate.of(2026, 5, 22)));

        SolveEntity solve = new SolveEntity(member, 5, 3, 60);
        solveRepository.save(solve);
        Long solveId = solve.getId();

        PaymentEntity payment = paymentRepository.save(new PaymentEntity(
                "withdraw-payment-1", memberId, null, "MuneoCBT Thunder", SubscriptionPlan.THREE_DAY, 4900));
        em.flush();
        Long paymentId = payment.getId();

        SubscriptionEntity subscription = subscriptionRepository.save(new SubscriptionEntity(
                memberId, SubscriptionPlan.THREE_DAY, paymentId, LocalDateTime.now(), LocalDateTime.now().plusDays(3)));
        MockExamPurchaseEntity purchase = mockExamPurchaseRepository.save(new MockExamPurchaseEntity(
                memberId, 100L, paymentId, LocalDateTime.now()));
        SubscriptionHistoryEntity history = subscriptionHistoryRepository.save(new SubscriptionHistoryEntity(
                memberId, SubscriptionPlan.THREE_DAY, SubscriptionHistoryAction.GRANTED,
                "withdraw test", null, paymentId, LocalDateTime.now()));

        em.flush();
        Long subscriptionId = subscription.getId();
        Long purchaseId = purchase.getId();
        Long historyId = history.getId();
        em.clear();

        // when
        memberService.withdraw(memberId);
        em.flush();
        em.clear();

        // then
        assertThat(memberRepository.findById(memberId)).isEmpty();
        assertThat(notificationRepository.countByMemberIdAndReadAtIsNull(memberId)).isZero();
        assertThat(solveRepository.findById(solveId)).isEmpty();
        assertThat(bookmarkRepository.countByMemberId(memberId)).isZero();
        assertThat(dailyUsageRepository.findAll()).isEmpty();

        assertThat(mockExamPurchaseRepository.findMockExamIdsByMemberId(memberId)).isEmpty();
        assertThat(subscriptionRepository.findActiveByMemberId(memberId, LocalDateTime.now())).isEmpty();
        assertThat(subscriptionHistoryRepository.findByMemberIdOrderByOccurredAtDesc(memberId)).isEmpty();

        assertThat(paymentRepository.findById(paymentId)).get().extracting(PaymentEntity::getMemberId).isNull();
        assertThat(subscriptionRepository.findById(subscriptionId)).get().extracting(SubscriptionEntity::getMemberId).isNull();
        assertThat(mockExamPurchaseRepository.findById(purchaseId)).get().extracting(MockExamPurchaseEntity::getMemberId).isNull();
        assertThat(subscriptionHistoryRepository.findById(historyId)).get()
                .extracting(SubscriptionHistoryEntity::getMemberId).isNull();

        var feedbacks = feedbackRepository.findAll();
        assertThat(feedbacks).hasSize(1);
        assertThat(feedbacks.get(0).getMemberId()).isNull();
        assertThat(feedbacks.get(0).getContent()).isEqualTo("keep me");
    }
}

package com.sqldpass.service.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.feedback.FeedbackEntity;
import com.sqldpass.persistent.feedback.FeedbackRepository;
import com.sqldpass.persistent.feedback.FeedbackType;
import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.notification.NotificationRepository;
import com.sqldpass.persistent.solve.SolveEntity;
import com.sqldpass.persistent.solve.SolveRepository;
import com.sqldpass.service.notification.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Transactional
class MemberServiceWithdrawTest {

    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private FeedbackRepository feedbackRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private SolveRepository solveRepository;
    @Autowired private jakarta.persistence.EntityManager em;

    @Test
    @DisplayName("withdraw — 회원 삭제, 알림 삭제, 풀이 삭제, 피드백은 memberId null 로 보존")
    void withdraw_endToEnd() {
        // given
        MemberEntity member = memberRepository.save(
                new MemberEntity("google", "providerXYZ", "탈퇴테스터"));
        Long memberId = member.getId();

        feedbackRepository.save(new FeedbackEntity(FeedbackType.OTHER, memberId, null, "보존됨", null));
        notificationService.notify(memberId, "T", "n1", null, null, null);
        notificationService.notify(memberId, "T", "n2", null, null, null);

        SolveEntity solve = new SolveEntity(member, (com.sqldpass.persistent.subject.SubjectEntity) null, 5, 3, 60);
        solveRepository.save(solve);
        Long solveId = solve.getId();

        em.flush();
        em.clear();

        // when
        memberService.withdraw(memberId);
        em.flush();
        em.clear();

        // then
        assertThat(memberRepository.findById(memberId)).isEmpty();
        assertThat(notificationRepository.countByMemberIdAndReadAtIsNull(memberId)).isZero();
        assertThat(solveRepository.findById(solveId)).isEmpty();

        // 피드백은 보존되고 memberId 만 null
        var feedbacks = feedbackRepository.findAll();
        assertThat(feedbacks).hasSize(1);
        assertThat(feedbacks.get(0).getMemberId()).isNull();
        assertThat(feedbacks.get(0).getContent()).isEqualTo("보존됨");
    }
}

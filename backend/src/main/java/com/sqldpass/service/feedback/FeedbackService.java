package com.sqldpass.service.feedback;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.feedback.dto.CreateFeedbackRequest;
import com.sqldpass.domain.feedback.Feedback;
import com.sqldpass.persistent.feedback.FeedbackEntity;
import com.sqldpass.persistent.feedback.FeedbackMapper;
import com.sqldpass.persistent.feedback.FeedbackRepository;
import com.sqldpass.persistent.feedback.FeedbackStatus;
import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.notification.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final MemberRepository memberRepository;
    private final QuestionRepository questionRepository;
    private final DiscordNotifier discordNotifier;
    private final NotificationService notificationService;

    @Transactional
    public Feedback create(Long memberId, CreateFeedbackRequest req) {
        FeedbackEntity entity = new FeedbackEntity(
                req.type(),
                memberId,
                req.questionId(),
                req.content(),
                req.pageUrl());
        FeedbackEntity saved = feedbackRepository.save(entity);

        // Discord 알림 (sendAsync 내부 try-catch 로 격리되어 본 트랜잭션에 영향 없음)
        try {
            String nickname = memberRepository.findById(memberId)
                    .map(MemberEntity::getNickname)
                    .orElse("?");
            String questionSummary = req.questionId() != null
                    ? questionRepository.findById(req.questionId())
                            .map(QuestionEntity::getSummary)
                            .orElse(null)
                    : null;
            discordNotifier.notifyFeedback(saved, nickname, questionSummary);
        } catch (Exception e) {
            log.warn("피드백 디스코드 알림 준비 실패 (저장은 성공): {}", e.getMessage());
        }

        return FeedbackMapper.toDomain(saved);
    }

    public Page<Feedback> getAll(FeedbackStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FeedbackEntity> result = (status != null)
                ? feedbackRepository.findByStatus(status, pageable)
                : feedbackRepository.findAllByOrderByCreatedAtDesc(pageable);
        return result.map(FeedbackMapper::toDomain);
    }

    @Transactional
    public Feedback updateStatus(Long id, FeedbackStatus status) {
        FeedbackEntity entity = feedbackRepository.findById(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.FEEDBACK_NOT_FOUND));
        FeedbackStatus prev = entity.getStatus();
        entity.changeStatus(status);

        if (status == FeedbackStatus.RESOLVED && prev != FeedbackStatus.RESOLVED) {
            String preview = entity.getContent();
            if (preview != null && preview.length() > 60) {
                preview = preview.substring(0, 60) + "…";
            }
            notificationService.notify(
                    entity.getMemberId(),
                    "FEEDBACK_RESOLVED",
                    "건의사항이 해결되었습니다",
                    preview,
                    "/mypage/feedback",
                    entity.getId());
        }

        return FeedbackMapper.toDomain(entity);
    }

    public String resolveNickname(Long memberId) {
        if (memberId == null) return "탈퇴한 회원";
        return memberRepository.findById(memberId)
                .map(MemberEntity::getNickname)
                .orElse("탈퇴한 회원");
    }
}

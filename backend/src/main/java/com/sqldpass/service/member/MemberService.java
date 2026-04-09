package com.sqldpass.service.member;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.member.dto.MemberMeResponse;
import com.sqldpass.persistent.feedback.FeedbackRepository;
import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.notification.NotificationRepository;
import com.sqldpass.persistent.solve.SolveEntity;
import com.sqldpass.persistent.solve.SolveRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import java.util.List;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final FeedbackRepository feedbackRepository;
    private final NotificationRepository notificationRepository;
    private final SolveRepository solveRepository;

    @Transactional(readOnly = true)
    public MemberMeResponse getMe(Long memberId) {
        MemberEntity entity = memberRepository.findById(memberId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MEMBER_NOT_FOUND));
        return toResponse(entity);
    }

    @Transactional
    public MemberMeResponse updateNickname(Long memberId, String nickname) {
        MemberEntity entity = memberRepository.findById(memberId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MEMBER_NOT_FOUND));

        // 같은 닉네임을 다른 회원이 이미 사용 중이면 409
        memberRepository.findByNickname(nickname).ifPresent(other -> {
            if (!other.getId().equals(memberId)) {
                throw new SqldpassException(ErrorCode.NICKNAME_DUPLICATE);
            }
        });

        try {
            entity.changeNickname(nickname);
            memberRepository.flush(); // 트랜잭션 커밋 전에 유니크 제약 위반 감지
        } catch (DataIntegrityViolationException e) {
            throw new SqldpassException(ErrorCode.NICKNAME_DUPLICATE);
        }

        return toResponse(entity);
    }

    /**
     * 회원 탈퇴 (hard delete).
     * - Notification: 본인 알림 모두 삭제
     * - Feedback: member_id 만 null 처리 (운영상 보존, '탈퇴한 회원'으로 표시)
     * - Solve: 본인 풀이 모두 삭제 (SolveAnswerEntity 는 orphanRemoval 로 cascade)
     * - Member: row 삭제
     */
    @Transactional
    public void withdraw(Long memberId) {
        MemberEntity entity = memberRepository.findById(memberId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MEMBER_NOT_FOUND));

        notificationRepository.deleteAllByMemberId(memberId);
        feedbackRepository.nullifyMember(memberId);

        List<SolveEntity> solves = solveRepository.findAllByMember_Id(memberId);
        if (!solves.isEmpty()) {
            solveRepository.deleteAll(solves);
        }

        memberRepository.delete(entity);
    }

    private MemberMeResponse toResponse(MemberEntity entity) {
        return new MemberMeResponse(
                entity.getId(),
                entity.getNickname(),
                entity.getProvider(),
                entity.getCreatedAt());
    }
}

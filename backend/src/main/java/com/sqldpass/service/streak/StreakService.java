package com.sqldpass.service.streak;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.streak.dto.StreakResponse;
import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberEntity.StreakUpdateResult;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StreakService {

    private final MemberRepository memberRepository;

    @Transactional
    public StreakUpdateResult updateOnSolve(Long memberId) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MEMBER_NOT_FOUND));
        return member.applyTodaySolve(LocalDate.now());
    }

    public StreakResponse getMyStreak(Long memberId) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MEMBER_NOT_FOUND));
        LocalDate today = LocalDate.now();
        return new StreakResponse(
                member.getCurrentStreak(),
                member.getLongestStreak(),
                member.getLastSolveDate(),
                member.hasSolvedToday(today));
    }
}

package com.sqldpass.service.member;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.member.dto.MemberMeResponse;
import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

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

    private MemberMeResponse toResponse(MemberEntity entity) {
        return new MemberMeResponse(
                entity.getId(),
                entity.getNickname(),
                entity.getProvider(),
                entity.getCreatedAt());
    }
}

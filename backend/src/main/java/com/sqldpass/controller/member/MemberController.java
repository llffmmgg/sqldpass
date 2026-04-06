package com.sqldpass.controller.member;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.member.dto.MemberMeResponse;
import com.sqldpass.controller.member.dto.UpdateNicknameRequest;
import com.sqldpass.service.member.MemberService;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "회원", description = "본인 회원 정보 API")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회")
    public MemberMeResponse getMe(@RequestAttribute("memberId") Long memberId) {
        return memberService.getMe(memberId);
    }

    @PatchMapping("/me/nickname")
    @Operation(summary = "닉네임 변경")
    public MemberMeResponse updateNickname(
            @RequestAttribute("memberId") Long memberId,
            @Valid @RequestBody UpdateNicknameRequest request) {
        return memberService.updateNickname(memberId, request.nickname());
    }
}

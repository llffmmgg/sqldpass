package com.sqldpass.service.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.admin.dto.AdminMemberResponse;
import com.sqldpass.persistent.member.MemberRepository;

@Service
@Transactional(readOnly = true)
public class AdminMemberService {

    private final MemberRepository memberRepository;

    public AdminMemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Page<AdminMemberResponse> getMembers(int page, int size) {
        return memberRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(AdminMemberResponse::from);
    }
}

package com.sqldpass.service.member;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.member.dto.MemberMeResponse;
import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Transactional
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    private MemberEntity member;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        member = memberRepository.save(new MemberEntity("google", "user-1", "tester"));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("getMe returns the persisted member")
    void getMe() {
        MemberMeResponse response = memberService.getMe(member.getId());

        assertThat(response.id()).isEqualTo(member.getId());
        assertThat(response.nickname()).isEqualTo("tester");
        assertThat(response.provider()).isEqualTo("google");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("updateNickname changes the member nickname")
    void updateNickname() {
        MemberMeResponse response = memberService.updateNickname(member.getId(), "renamed");

        entityManager.flush();
        entityManager.clear();

        assertThat(response.nickname()).isEqualTo("renamed");
        assertThat(memberRepository.findById(member.getId()))
                .get()
                .extracting(MemberEntity::getNickname)
                .isEqualTo("renamed");
    }

    @Test
    @DisplayName("updateNickname throws when the nickname is already used")
    void updateNickname_duplicate() {
        memberRepository.save(new MemberEntity("google", "user-2", "already-used"));
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> memberService.updateNickname(member.getId(), "already-used"))
                .isInstanceOf(SqldpassException.class)
                .extracting(ex -> ((SqldpassException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NICKNAME_DUPLICATE);
    }
}

package com.sqldpass.service.auth;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.notification.DiscordNotifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * AuthService.upsertMemberAndIssueToken 의 email 처리 분기 회귀 방지.
 * V82 (2026-05-11) email 컬럼 재추가 + Google OAuth email_verified 통과분만 저장 정책.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceEmailTest {

    @Mock
    private GoogleOAuthClient googleOAuthClient;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private DiscordNotifier discordNotifier;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(googleOAuthClient, memberRepository, jwtProvider, discordNotifier);
        // 신규 가입 시 save() 반환 객체의 id 가 null 일 수 있어 nullable 매처 사용.
        given(jwtProvider.createUserToken(nullable(Long.class))).willReturn("jwt-token");
    }

    @Test
    @DisplayName("신규 가입: GoogleUserInfo.email 이 verified 면 4-인자 생성자로 저장")
    void newMemberSavesEmailFromVerifiedGoogleInfo() {
        given(googleOAuthClient.verifyIdToken("id-token-1"))
                .willReturn(new GoogleOAuthClient.GoogleUserInfo("sub-1", "홍길동", "user@example.com"));
        given(memberRepository.findByProviderAndProviderId("google", "sub-1"))
                .willReturn(Optional.empty());
        given(memberRepository.save(any(MemberEntity.class)))
                .willAnswer(inv -> inv.getArgument(0));

        AuthService.AuthResult result = service.loginWithGoogleIdToken("id-token-1");

        ArgumentCaptor<MemberEntity> captor = ArgumentCaptor.forClass(MemberEntity.class);
        verify(memberRepository, times(1)).save(captor.capture());
        MemberEntity saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getProvider()).isEqualTo("google");
        assertThat(saved.getProviderId()).isEqualTo("sub-1");
        assertThat(result.isNew()).isTrue();
        verify(discordNotifier, times(1)).notifyNewMember(any(MemberEntity.class));
    }

    @Test
    @DisplayName("신규 가입: GoogleUserInfo.email 이 null (verified=false) 이면 email 없이 저장")
    void newMemberSavesNullEmailWhenGoogleNotVerified() {
        given(googleOAuthClient.verifyIdToken("id-token-2"))
                .willReturn(new GoogleOAuthClient.GoogleUserInfo("sub-2", "이몽룡", null));
        given(memberRepository.findByProviderAndProviderId("google", "sub-2"))
                .willReturn(Optional.empty());
        given(memberRepository.save(any(MemberEntity.class)))
                .willAnswer(inv -> inv.getArgument(0));

        service.loginWithGoogleIdToken("id-token-2");

        ArgumentCaptor<MemberEntity> captor = ArgumentCaptor.forClass(MemberEntity.class);
        verify(memberRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isNull();
    }

    @Test
    @DisplayName("기존 회원 재로그인: email NULL 이고 verified email 받으면 updateEmail 호출됨 (백필)")
    void existingMemberBackfillsEmailWhenNullAndVerifiedReceived() {
        MemberEntity existing = new MemberEntity("google", "sub-3", "기존닉");
        assertThat(existing.getEmail()).isNull(); // 가드
        given(googleOAuthClient.verifyIdToken("id-token-3"))
                .willReturn(new GoogleOAuthClient.GoogleUserInfo("sub-3", "기존회원", "new@example.com"));
        given(memberRepository.findByProviderAndProviderId("google", "sub-3"))
                .willReturn(Optional.of(existing));

        service.loginWithGoogleIdToken("id-token-3");

        // dirty checking 으로 자동 UPDATE → 명시적 save 호출은 없음, 객체 상태로 검증
        assertThat(existing.getEmail()).isEqualTo("new@example.com");
        verify(memberRepository, never()).save(any(MemberEntity.class));
        verify(discordNotifier, never()).notifyNewMember(any(MemberEntity.class));
    }

    @Test
    @DisplayName("기존 회원 재로그인: email 동일하면 updateEmail 내부 단락 — 상태 유지")
    void existingMemberNoOpWhenEmailSame() {
        MemberEntity existing = new MemberEntity("google", "sub-4", "기존닉", "same@example.com");
        given(googleOAuthClient.verifyIdToken("id-token-4"))
                .willReturn(new GoogleOAuthClient.GoogleUserInfo("sub-4", "기존회원", "same@example.com"));
        given(memberRepository.findByProviderAndProviderId("google", "sub-4"))
                .willReturn(Optional.of(existing));

        service.loginWithGoogleIdToken("id-token-4");

        assertThat(existing.getEmail()).isEqualTo("same@example.com");
        verify(memberRepository, never()).save(any(MemberEntity.class));
    }

    @Test
    @DisplayName("기존 회원 재로그인: 받아온 email 이 null (verified=false) 이면 기존 email 유지")
    void existingMemberKeepsEmailWhenIncomingNull() {
        MemberEntity existing = new MemberEntity("google", "sub-5", "기존닉", "keep@example.com");
        given(googleOAuthClient.verifyIdToken("id-token-5"))
                .willReturn(new GoogleOAuthClient.GoogleUserInfo("sub-5", "기존회원", null));
        given(memberRepository.findByProviderAndProviderId("google", "sub-5"))
                .willReturn(Optional.of(existing));

        service.loginWithGoogleIdToken("id-token-5");

        // null 입력 → updateEmail 단락 → 기존 값 유지
        assertThat(existing.getEmail()).isEqualTo("keep@example.com");
    }
}

package com.sqldpass.service.auth;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.notification.DiscordNotifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private GoogleOAuthClient googleOAuthClient;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private DiscordNotifier discordNotifier;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("loginWithGoogle returns an existing member token")
    void loginWithGoogle_existingMember() {
        MemberEntity member = new MemberEntity("google", "google-sub", "tester");
        setId(member, 1L);

        given(googleOAuthClient.exchangeCode("oauth-code", "https://app.example/callback")).willReturn("access-token");
        given(googleOAuthClient.getUserInfo("access-token"))
                .willReturn(new GoogleOAuthClient.GoogleUserInfo("google-sub", "Tester"));
        given(memberRepository.findByProviderAndProviderId("google", "google-sub")).willReturn(Optional.of(member));
        given(jwtProvider.createUserToken(1L)).willReturn("jwt-token");

        AuthService.AuthResult result = authService.loginWithGoogle("oauth-code", "https://app.example/callback");

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.nickname()).isEqualTo("tester");
        assertThat(result.isNew()).isFalse();
        then(discordNotifier).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("loginWithGoogle creates and notifies for a new member")
    void loginWithGoogle_newMember() {
        MemberEntity saved = new MemberEntity("google", "new-sub-1234567890123", "user_new-sub-123");
        setId(saved, 3L);

        given(googleOAuthClient.exchangeCode("oauth-code", "https://app.example/callback")).willReturn("access-token");
        given(googleOAuthClient.getUserInfo("access-token"))
                .willReturn(new GoogleOAuthClient.GoogleUserInfo("new-sub-1234567890123", "New User"));
        given(memberRepository.findByProviderAndProviderId("google", "new-sub-1234567890123"))
                .willReturn(Optional.empty());
        given(memberRepository.save(any(MemberEntity.class))).willReturn(saved);
        given(jwtProvider.createUserToken(3L)).willReturn("jwt-token");

        AuthService.AuthResult result = authService.loginWithGoogle("oauth-code", "https://app.example/callback");

        ArgumentCaptor<MemberEntity> captor = ArgumentCaptor.forClass(MemberEntity.class);
        then(memberRepository).should().save(captor.capture());
        then(discordNotifier).should().notifyNewMember(saved);

        assertThat(captor.getValue().getProvider()).isEqualTo("google");
        assertThat(captor.getValue().getProviderId()).isEqualTo("new-sub-1234567890123");
        assertThat(captor.getValue().getNickname()).startsWith("user_");
        assertThat(result.isNew()).isTrue();
        assertThat(result.nickname()).isEqualTo(saved.getNickname());
    }

    private static void setId(MemberEntity member, Long id) {
        try {
            var field = MemberEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(member, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}

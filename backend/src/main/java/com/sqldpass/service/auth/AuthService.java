package com.sqldpass.service.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.service.admin.JwtProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleOAuthClient googleOAuthClient;
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public AuthResult loginWithGoogle(String code, String redirectUri) {
        String accessToken = googleOAuthClient.exchangeCode(code, redirectUri);
        GoogleOAuthClient.GoogleUserInfo userInfo = googleOAuthClient.getUserInfo(accessToken);

        MemberEntity member = memberRepository.findByProviderAndProviderId("google", userInfo.sub())
                .orElseGet(() -> memberRepository.save(
                        new MemberEntity("google", userInfo.sub(), userInfo.name())));

        String token = jwtProvider.createUserToken(member.getId());
        return new AuthResult(token, member.getNickname());
    }

    public record AuthResult(String token, String nickname) {
    }
}

package com.sqldpass.service.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.notification.DiscordNotifier;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleOAuthClient googleOAuthClient;
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;
    private final DiscordNotifier discordNotifier;

    @Transactional
    public AuthResult loginWithGoogle(String code, String redirectUri) {
        String accessToken = googleOAuthClient.exchangeCode(code, redirectUri);
        GoogleOAuthClient.GoogleUserInfo userInfo = googleOAuthClient.getUserInfo(accessToken);
        return upsertMemberAndIssueToken(userInfo);
    }

    /**
     * 네이티브 안드로이드 앱 — Google Sign-In 이 ID 토큰을 직접 반환하므로
     * code↔access_token 교환 단계가 없다. ID 토큰 자체를 검증하고 같은 흐름으로 회원 발급.
     */
    @Transactional
    public AuthResult loginWithGoogleIdToken(String idToken) {
        GoogleOAuthClient.GoogleUserInfo userInfo = googleOAuthClient.verifyIdToken(idToken);
        return upsertMemberAndIssueToken(userInfo);
    }

    private AuthResult upsertMemberAndIssueToken(GoogleOAuthClient.GoogleUserInfo userInfo) {
        boolean[] isNew = {false};
        MemberEntity member = memberRepository.findByProviderAndProviderId("google", userInfo.sub())
                .orElseGet(() -> {
                    isNew[0] = true;
                    // 신규 가입 시 임시 닉네임 (providerId 일부 사용 → 유니크 보장)
                    // 프론트엔드가 즉시 korean-random-words로 생성한 닉네임으로 PATCH할 예정
                    String placeholder = "user_" + userInfo.sub().substring(0, Math.min(12, userInfo.sub().length()));
                    return memberRepository.save(
                            new MemberEntity("google", userInfo.sub(), placeholder));
                });

        // Discord 알림 — 신규 가입 시점에만
        if (isNew[0]) {
            discordNotifier.notifyNewMember(member);
        }

        String token = jwtProvider.createUserToken(member.getId());
        return new AuthResult(token, member.getNickname(), isNew[0]);
    }

    public record AuthResult(String token, String nickname, boolean isNew) {
    }
}

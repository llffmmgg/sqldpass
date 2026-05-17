package com.sqldpass.controller.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * iOS Sign in with Apple — 네이티브 ASAuthorizationAppleIDProvider 가
 * 발급한 ID Token 을 검증해 회원 식별자(sub)로 사용한다.
 *
 * @param idToken            JWS 형식의 Apple ID Token
 * @param authorizationCode  단일 사용 코드 — 추후 refresh token 발급 시 사용 (옵션)
 * @param nickname           Apple 이 최초 1회만 제공하는 사용자 이름 (옵션)
 */
public record AppleLoginRequest(
        @NotBlank String idToken,
        String authorizationCode,
        String nickname
) {
}

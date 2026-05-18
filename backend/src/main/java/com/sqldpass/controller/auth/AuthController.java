package com.sqldpass.controller.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.auth.dto.AppleLoginRequest;
import com.sqldpass.controller.auth.dto.GoogleIdTokenLoginRequest;
import com.sqldpass.controller.auth.dto.OAuthLoginRequest;
import com.sqldpass.controller.auth.dto.OAuthLoginResponse;
import com.sqldpass.controller.auth.dto.TokenRefreshResponse;
import com.sqldpass.service.auth.AuthService;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "인증", description = "소셜 로그인 API")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/api/auth/login/google")
    @Operation(summary = "Google 소셜 로그인 — 웹 OAuth code 흐름")
    public OAuthLoginResponse loginWithGoogle(@Valid @RequestBody OAuthLoginRequest request) {
        AuthService.AuthResult result = authService.loginWithGoogle(request.code(), request.redirectUri());
        return new OAuthLoginResponse(result.token(), result.nickname(), result.isNew());
    }

    @PostMapping("/api/auth/login/google/idtoken")
    @Operation(summary = "Google 소셜 로그인 — 안드로이드 앱 네이티브 ID 토큰 흐름")
    public OAuthLoginResponse loginWithGoogleIdToken(
            @Valid @RequestBody GoogleIdTokenLoginRequest request) {
        AuthService.AuthResult result = authService.loginWithGoogleIdToken(request.idToken());
        return new OAuthLoginResponse(result.token(), result.nickname(), result.isNew());
    }

    @PostMapping("/api/auth/login/apple")
    @Operation(summary = "Sign in with Apple — iOS 앱 네이티브 ID 토큰 흐름")
    public OAuthLoginResponse loginWithApple(
            @Valid @RequestBody AppleLoginRequest request) {
        AuthService.AuthResult result = authService.loginWithApple(request.idToken());
        return new OAuthLoginResponse(result.token(), result.nickname(), result.isNew());
    }

    @PostMapping("/api/auth/refresh")
    @Operation(summary = "JWT 재발급 — 현재 유효한 토큰을 새 만료시간으로 갱신")
    public TokenRefreshResponse refresh(@RequestAttribute("memberId") Long memberId) {
        AuthService.TokenRefreshResult result = authService.reissueToken(memberId);
        return new TokenRefreshResponse(result.token(), result.nickname());
    }
}

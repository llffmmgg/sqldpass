package com.sqldpass.controller.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.auth.dto.OAuthLoginRequest;
import com.sqldpass.controller.auth.dto.OAuthLoginResponse;
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
    @Operation(summary = "Google 소셜 로그인")
    public OAuthLoginResponse loginWithGoogle(@Valid @RequestBody OAuthLoginRequest request) {
        AuthService.AuthResult result = authService.loginWithGoogle(request.code(), request.redirectUri());
        return new OAuthLoginResponse(result.token(), result.nickname());
    }
}

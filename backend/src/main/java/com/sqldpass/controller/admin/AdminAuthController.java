package com.sqldpass.controller.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.admin.dto.LoginRequest;
import com.sqldpass.controller.admin.dto.LoginResponse;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "관리자 인증", description = "관리자 로그인 API")
@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private final JwtProvider jwtProvider;
    private final String adminUsername;
    private final String adminPassword;

    public AdminAuthController(
            JwtProvider jwtProvider,
            @Value("${sqldpass.admin.username}") String adminUsername,
            @Value("${sqldpass.admin.password}") String adminPassword) {
        this.jwtProvider = jwtProvider;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @PostMapping("/login")
    @Operation(summary = "관리자 로그인", description = "관리자 계정으로 로그인하여 JWT 토큰을 발급받는다")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        if (!adminUsername.equals(request.username()) || !adminPassword.equals(request.password())) {
            throw new SqldpassException(ErrorCode.ADMIN_LOGIN_FAILED);
        }
        String token = jwtProvider.createToken(request.username());
        return new LoginResponse(token);
    }
}

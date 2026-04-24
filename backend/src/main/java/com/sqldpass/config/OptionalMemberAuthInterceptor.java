package com.sqldpass.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.sqldpass.service.admin.JwtProvider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 선택적 회원 인증 인터셉터.
 *
 * Bearer 토큰이 있으면 memberId 를 request attribute 에 주입하고,
 * 없거나 유효하지 않으면 그대로 통과시킨다 (401 반환하지 않음).
 *
 * 비로그인도 접근 가능하지만 로그인 사용자에게는 추가 정보를 주는
 * 엔드포인트(예: /api/public/past-exams) 에서 사용한다.
 */
@Component
public class OptionalMemberAuthInterceptor implements HandlerInterceptor {

    private final JwtProvider jwtProvider;

    public OptionalMemberAuthInterceptor(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return true;
        }

        String token = authHeader.substring(7);
        try {
            if (jwtProvider.validateToken(token)) {
                Long memberId = jwtProvider.extractMemberId(token);
                request.setAttribute("memberId", memberId);
            }
        } catch (Exception ignored) {
            // 토큰 파싱 실패 — 비로그인으로 간주하고 통과
        }
        return true;
    }
}

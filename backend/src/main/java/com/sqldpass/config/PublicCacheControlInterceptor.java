package com.sqldpass.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 회원 의존성이 없는 공개 GET 응답에 한해
 *   Cache-Control: public, max-age=1800
 * 헤더를 부착해 브라우저·CDN 재사용을 허용한다.
 *
 * 적용 범위는 {@link WebMvcConfig#addInterceptors}의 화이트리스트로 제한한다.
 * 회원/IP 별 응답이 달라질 수 있는 경로(예: /past-exams 목록, /random-questions)는
 * 화이트리스트에서 제외했다.
 */
@Component
public class PublicCacheControlInterceptor implements HandlerInterceptor {

    private static final String CACHE_HEADER_VALUE = "public, max-age=1800";

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) return;
        if (response.containsHeader("Cache-Control")) return;
        if (response.getStatus() != HttpServletResponse.SC_OK) return;
        response.setHeader("Cache-Control", CACHE_HEADER_VALUE);
    }
}

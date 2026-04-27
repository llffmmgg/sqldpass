package com.sqldpass.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;
    private final MemberAuthInterceptor memberAuthInterceptor;
    private final OptionalMemberAuthInterceptor optionalMemberAuthInterceptor;

    public WebMvcConfig(AdminAuthInterceptor adminAuthInterceptor,
                        MemberAuthInterceptor memberAuthInterceptor,
                        OptionalMemberAuthInterceptor optionalMemberAuthInterceptor) {
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.memberAuthInterceptor = memberAuthInterceptor;
        this.optionalMemberAuthInterceptor = optionalMemberAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/login");

        registry.addInterceptor(memberAuthInterceptor)
                .addPathPatterns(
                        "/api/solves/**",
                        "/api/wrong-answers/**",
                        "/api/bookmarks/**",
                        "/api/members/**",
                        "/api/mock-exams/**",
                        "/api/questions/**",
                        "/api/feedback/**",
                        "/api/notifications/**",
                        "/api/streak/**");

        // 비로그인도 접근 가능하지만 로그인 시 memberId 를 부가적으로 주입.
        // /api/posts: GET 은 비로그인 허용, POST/PATCH/DELETE 는 controller 안에서 memberId null 체크.
        registry.addInterceptor(optionalMemberAuthInterceptor)
                .addPathPatterns(
                        "/api/public/past-exams/**",
                        "/api/posts/**");
    }
}

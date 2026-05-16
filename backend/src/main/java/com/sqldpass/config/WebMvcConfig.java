package com.sqldpass.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;
    private final MemberAuthInterceptor memberAuthInterceptor;
    private final OptionalMemberAuthInterceptor optionalMemberAuthInterceptor;
    private final PublicCacheControlInterceptor publicCacheControlInterceptor;

    public WebMvcConfig(AdminAuthInterceptor adminAuthInterceptor,
                        MemberAuthInterceptor memberAuthInterceptor,
                        OptionalMemberAuthInterceptor optionalMemberAuthInterceptor,
                        PublicCacheControlInterceptor publicCacheControlInterceptor) {
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.memberAuthInterceptor = memberAuthInterceptor;
        this.optionalMemberAuthInterceptor = optionalMemberAuthInterceptor;
        this.publicCacheControlInterceptor = publicCacheControlInterceptor;
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
                        "/api/streak/**",
                        "/api/uploads/**",
                        "/api/payment/**",
                        "/api/blog-downloads/**");

        // 비로그인도 접근 가능하지만 로그인 시 memberId 를 부가적으로 주입.
        // /api/posts: GET 은 비로그인 허용, POST/PATCH/DELETE 는 controller 안에서 memberId null 체크.
        registry.addInterceptor(optionalMemberAuthInterceptor)
                .addPathPatterns(
                        "/api/public/past-exams/**",
                        "/api/posts/**");

        // 회원/IP 의존성이 없는 공개 GET 응답에만 Cache-Control: public, max-age=1800.
        // 의존성이 있는 경로(예: /past-exams 목록의 best-score, /random-questions의 IP 한도)는
        // 화이트리스트에서 의도적으로 제외.
        registry.addInterceptor(publicCacheControlInterceptor)
                .addPathPatterns(
                        "/api/public/stats",
                        "/api/public/ranking",
                        "/api/public/insights/hardest",
                        "/api/public/certs",
                        "/api/public/certs/*/categories",
                        "/api/public/categories/*/questions",
                        "/api/public/questions/*",
                        "/api/public/daily-question",
                        "/api/public/subjects",
                        "/api/public/mock-exams",
                        "/api/public/past-exams/*/with-answers",
                        "/api/public/blog/views",
                        "/api/public/posts/*",
                        "/api/public/posts/seo-list",
                        "/api/notices/active");
    }
}

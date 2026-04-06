package com.sqldpass.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;
    private final MemberAuthInterceptor memberAuthInterceptor;

    public WebMvcConfig(AdminAuthInterceptor adminAuthInterceptor, MemberAuthInterceptor memberAuthInterceptor) {
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.memberAuthInterceptor = memberAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/login");

        registry.addInterceptor(memberAuthInterceptor)
                .addPathPatterns("/api/solves/**", "/api/wrong-answers/**", "/api/members/**", "/api/mock-exams/**");
    }
}

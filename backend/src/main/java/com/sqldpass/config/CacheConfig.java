package com.sqldpass.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Caffeine 기반 인메모리 캐시 설정.
 *
 * 캐시별 정책 분리:
 *  - publicStats / publicRanking / hardestQuestions : 1시간 TTL (기존)
 *  - publicCerts / publicCategories / mockExamList  : 30분 TTL (신규, 사용자 합의)
 *
 * 신규 캐시는 운영 중 변경 가능성이 있어(모의고사 visibility 토글 등)
 * 작성/삭제 경로에서 @CacheEvict 로 즉시 무효화한다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_PUBLIC_STATS = "publicStats";
    public static final String CACHE_PUBLIC_RANKING = "publicRanking";
    public static final String CACHE_HARDEST_QUESTIONS = "hardestQuestions";
    public static final String CACHE_PUBLIC_CERTS = "publicCerts";
    public static final String CACHE_PUBLIC_CATEGORIES = "publicCategories";
    public static final String CACHE_MOCK_EXAM_LIST = "mockExamList";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager();
        mgr.registerCustomCache(CACHE_PUBLIC_STATS, oneHourCache());
        mgr.registerCustomCache(CACHE_PUBLIC_RANKING, oneHourCache());
        mgr.registerCustomCache(CACHE_HARDEST_QUESTIONS, oneHourCache());
        mgr.registerCustomCache(CACHE_PUBLIC_CERTS, halfHourCache());
        mgr.registerCustomCache(CACHE_PUBLIC_CATEGORIES, halfHourCache());
        mgr.registerCustomCache(CACHE_MOCK_EXAM_LIST, halfHourCache());
        return mgr;
    }

    private Cache<Object, Object> oneHourCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(100)
                .build();
    }

    private Cache<Object, Object> halfHourCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();
    }
}

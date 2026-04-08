package com.sqldpass.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Caffeine 기반 인메모리 캐시 설정.
 *
 * 모든 공개 캐시는 단일 정책 (TTL 1시간, 최대 100 항목).
 * - publicStats: 랜딩 페이지 회원/풀이 카운트
 * - publicRanking: 랜딩 페이지 TOP 30 랭킹
 *
 * 향후 항목별 다른 TTL 필요하면 cache 이름별 spec 분리.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_PUBLIC_STATS = "publicStats";
    public static final String CACHE_PUBLIC_RANKING = "publicRanking";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager(
                CACHE_PUBLIC_STATS,
                CACHE_PUBLIC_RANKING
        );
        mgr.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(100));
        return mgr;
    }
}

package com.sqldpass.config;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 부팅 시 Flyway가 항상 repair() → migrate() 순서로 실행되게 한다.
 *
 * 이전 부팅에서 마이그레이션이 실패해 schema_history에 success=0 row가
 * 남으면 다음 부팅 시 validate 단계에서 거절되어 컨테이너가 못 뜸.
 * repair()가 실패 row를 정리해주므로 사람이 운영 DB를 만질 필요 없음.
 *
 * V22가 idempotent(컬럼/인덱스 존재 여부 체크)이므로 재실행해도 안전.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}

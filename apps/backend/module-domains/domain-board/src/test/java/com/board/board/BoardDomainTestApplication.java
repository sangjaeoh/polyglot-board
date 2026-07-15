package com.board.board;

import com.board.common.jpa.config.JpaAuditingConfig;
import com.board.common.jpa.flyway.SchemaFlyway;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** 도메인 통합 테스트의 스프링 진입점이다. 앱 조립 없이 JPA·Flyway·Auditing만 구성한다. */
@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration
@Import(JpaAuditingConfig.class)
class BoardDomainTestApplication {

    /** board 스키마로 Flyway를 구성한다(앱과 같은 배선 — 도메인 소유 마이그레이션을 실행한다). */
    @Bean
    FlywayConfigurationCustomizer boardSchemaCustomizer() {
        return configuration -> SchemaFlyway.applySchema(configuration, "board");
    }
}

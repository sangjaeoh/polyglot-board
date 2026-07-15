package com.board.api.config;

import com.board.common.jpa.flyway.SchemaFlyway;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * board 스키마로 Flyway를 구성한다.
 *
 * <p>Spring Boot Flyway 초기화가 이 커스터마이저가 구성한 인스턴스로 {@code migrate()}를 실행하고
 * EntityManagerFactory를 그 뒤에 오도록 순서화한다. 따라서 {@code ddl-auto=validate} 전에 스키마가 준비된다.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayConfigurationCustomizer boardSchemaCustomizer() {
        return configuration -> SchemaFlyway.applySchema(configuration, "board");
    }
}

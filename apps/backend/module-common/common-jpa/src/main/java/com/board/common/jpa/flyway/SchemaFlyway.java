package com.board.common.jpa.flyway;

import org.flywaydb.core.api.configuration.FluentConfiguration;

/**
 * 도메인 스키마별 Flyway 구성을 캡슐화한다.
 *
 * <p>스키마마다 자기 {@code flyway_schema_history}를 그 스키마 안에 두고, 마이그레이션 위치는
 * {@code classpath:db/migration/{schema}}다. 도메인 경계가 스키마로 드러나 크로스 도메인 조인이
 * 구조적으로 차단된다. 마이그레이션 실행·EMF 순서화는 Spring Boot Flyway 초기화가 소유한다.
 */
public final class SchemaFlyway {

    private SchemaFlyway() {}

    /** 주어진 Flyway 구성을 한 도메인 스키마에 격리시킨다. */
    public static void applySchema(FluentConfiguration configuration, String schema) {
        configuration
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration/" + schema)
                .createSchemas(true);
    }
}

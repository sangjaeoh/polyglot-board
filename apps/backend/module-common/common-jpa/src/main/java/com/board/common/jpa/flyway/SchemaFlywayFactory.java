package com.board.common.jpa.flyway;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

/**
 * 도메인 스키마별 Flyway 인스턴스를 만든다.
 *
 * <p>스키마마다 자기 {@code flyway_schema_history}를 그 스키마 안에 두고, 마이그레이션 위치는
 * {@code classpath:db/migration/{schema}}다. 도메인 경계가 스키마로 드러나 크로스 도메인 조인이
 * 구조적으로 차단된다. 실행은 app-migration이 소유한다 — 실행 앱은 부팅 시 Flyway를 실행하지 않는다.
 */
public final class SchemaFlywayFactory {

    private SchemaFlywayFactory() {}

    /** 도메인 스키마에 격리된 Flyway 인스턴스를 만든다(app-migration 실행 경로). */
    public static Flyway create(DataSource dataSource, String schema) {
        return configure(schema).dataSource(dataSource).load();
    }

    /** JDBC 접속 정보로 만든다 — 테스트 하네스가 컨테이너 기동 직후 스키마를 준비할 때 쓴다. */
    public static Flyway create(String url, String username, String password, String schema) {
        return configure(schema).dataSource(url, username, password).load();
    }

    private static FluentConfiguration configure(String schema) {
        return Flyway.configure()
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration/" + schema)
                .createSchemas(true);
    }
}

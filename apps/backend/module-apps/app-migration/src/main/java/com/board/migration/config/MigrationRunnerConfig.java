package com.board.migration.config;

import com.board.common.jpa.flyway.SchemaFlywayFactory;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 도메인 스키마 목록을 순회하며 {@link SchemaFlywayFactory} 인스턴스로 migrate를 실행한다.
 *
 * <p>스키마 목록은 이 앱의 선언 의존(도메인 모듈)을 미러링한다 — 도메인 지식은 common에 두지 않으므로
 * 등록 지점은 app-migration이 소유한다. 도메인 모듈을 추가하면 이 목록도 함께 갱신한다.
 */
@Configuration(proxyBeanMethods = false)
public class MigrationRunnerConfig {

    private static final List<String> DOMAIN_SCHEMAS = List.of("board");

    @Bean
    CommandLineRunner migrationRunner(DataSource dataSource) {
        return args -> {
            for (String schema : DOMAIN_SCHEMAS) {
                SchemaFlywayFactory.create(dataSource, schema).migrate();
            }
        };
    }
}

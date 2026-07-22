package com.board.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.board.migration.support.ContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * app-migration 실행으로 도메인 스키마가 생성됨을 검증한다.
 *
 * <p>{@code @SpringBootTest}는 SpringApplication.run 경유로 컨텍스트를 만들므로 CommandLineRunner
 * (마이그레이션 실행)가 테스트 전에 수행된다 — 이 하중에 기대는 테스트다.
 */
@SpringBootTest
@Import(ContainerConfig.class)
class MigrationApplicationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migratesDomainSchemas() {
        Integer applied = jdbcTemplate.queryForObject(
                "select count(*) from board.flyway_schema_history where success", Integer.class);
        assertThat(applied).isNotNull().isGreaterThan(0);

        Integer postTable = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'board' and table_name = 'post'",
                Integer.class);
        assertThat(postTable).isEqualTo(1);
    }
}

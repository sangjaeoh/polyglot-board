package com.board.app.api.support;

import com.board.common.jpa.flyway.SchemaFlywayFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 통합 테스트에 실 PostgreSQL(Testcontainers)을 제공한다. H2 divergence를 피한다.
 *
 * <p>실행 앱은 부팅 시 Flyway를 실행하지 않으므로(app-migration 소유), 컨테이너 기동 직후 여기서
 * 마이그레이션을 실행해 EMF({@code ddl-auto: validate}) 전에 스키마를 준비한다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ContainerConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:17-alpine");
        container.start();
        SchemaFlywayFactory.create(container.getJdbcUrl(), container.getUsername(), container.getPassword(), "board")
                .migrate();
        return container;
    }
}

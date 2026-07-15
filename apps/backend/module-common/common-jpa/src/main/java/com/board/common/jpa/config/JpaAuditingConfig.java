package com.board.common.jpa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** JPA Auditing을 활성화해 {@code created_at}·{@code updated_at}을 자동으로 채운다. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}

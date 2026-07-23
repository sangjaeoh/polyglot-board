package com.board.domain.board;

import com.board.common.jpa.config.JpaAuditingConfig;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

/** 도메인 통합 테스트의 스프링 진입점이다. 앱 조립 없이 JPA·Auditing만 구성한다(스키마는 ContainerConfig가 준비). */
@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration
@Import(JpaAuditingConfig.class)
class BoardDomainTestApplication {}

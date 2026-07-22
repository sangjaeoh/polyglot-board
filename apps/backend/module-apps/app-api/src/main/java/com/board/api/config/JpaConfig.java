package com.board.api.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** JPA 배선(엔티티 스캔·리포지토리 활성)을 앱 진입점에서 분리한다 — 웹 슬라이스(@WebMvcTest)가 JPA를 끌어오지 않게 한다. */
@Configuration(proxyBeanMethods = false)
@EntityScan("com.board")
@EnableJpaRepositories("com.board")
class JpaConfig {}

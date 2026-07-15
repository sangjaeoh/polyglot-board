package com.board.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** board API 앱의 진입점. 도메인·공용 모듈을 조립한다. */
@SpringBootApplication(scanBasePackages = "com.board")
@EntityScan("com.board")
@EnableJpaRepositories("com.board")
public class BoardApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoardApiApplication.class, args);
    }
}

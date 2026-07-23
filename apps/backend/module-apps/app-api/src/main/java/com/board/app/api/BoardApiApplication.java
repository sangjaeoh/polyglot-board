package com.board.app.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** board API 앱의 진입점. 도메인·공용 모듈을 조립한다. JPA 배선은 config의 JpaConfig가 소유한다. */
@SpringBootApplication(scanBasePackages = "com.board")
public class BoardApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoardApiApplication.class, args);
    }
}

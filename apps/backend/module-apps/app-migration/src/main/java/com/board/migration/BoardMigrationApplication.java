package com.board.migration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;

/**
 * Flyway 독립 실행기 — 마이그레이션만 실행하고 종료한다. 런타임 데이터 접근 없음.
 *
 * <p>JPA 오토컨피그는 common-jpa 전이로 클래스패스에 오르지만 이 앱의 책임 밖이라 제외한다("런타임
 * 데이터 접근 없음" 정합 위생 — 제외 없이도 기동은 되나 EMF가 불필요한 DB 커넥션을 연다).
 */
@SpringBootApplication(exclude = {HibernateJpaAutoConfiguration.class, DataJpaRepositoriesAutoConfiguration.class})
public class BoardMigrationApplication {

    public static void main(String[] args) {
        // 실행→migrate→종료. 러너 실패는 run()에서 전파돼 non-zero exit로 끝난다.
        System.exit(SpringApplication.exit(SpringApplication.run(BoardMigrationApplication.class, args)));
    }
}

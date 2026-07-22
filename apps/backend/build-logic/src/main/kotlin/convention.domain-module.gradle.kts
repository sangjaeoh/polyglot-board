// domain-{name} 모듈. JPA를 제공하고 허용 의존은 domain-shared·common-core·jpa·messaging로 화이트리스트한다.
plugins {
    id("convention.java-common")
}

dependencies {
    "implementation"(libs.lib("spring-boot-starter-data-jpa"))

    // 테스트 층 배선 — 단위(JUnit·Mockito)는 starter-test, 리포지토리 통합은 실 PostgreSQL(Testcontainers).
    "testImplementation"(libs.lib("spring-boot-starter-test"))
    "testImplementation"(libs.lib("spring-boot-testcontainers"))
    "testImplementation"(libs.lib("testcontainers-postgresql"))
    "testImplementation"(libs.lib("testcontainers-junit"))
    // 부트 플러그인 없는 라이브러리 모듈은 Gradle 9가 요구하는 launcher를 직접 싣는다(버전은 BOM 소유).
    "testRuntimeOnly"(libs.lib("junit-platform-launcher"))
    "testRuntimeOnly"(libs.lib("postgresql"))
}

// 타 도메인·infra·external·web·auth 의존 금지. 허용은 domain-shared·common-core·jpa·messaging만.
enforceAllowedProjectDependencies(
    "domain",
    listOf(
        ":module-domains:domain-shared",
        ":module-common:common-core",
        ":module-common:common-jpa",
        ":module-common:common-messaging",
    ),
)

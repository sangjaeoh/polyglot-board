plugins {
    id("convention.app-module")
}

dependencies {
    // 도메인 모듈이 마이그레이션 SQL(db/migration/{schema})을 클래스패스로 제공한다.
    implementation(project(":module-domains:domain-board"))
    // SchemaFlywayFactory(common-jpa) — 도메인 스키마별 Flyway 인스턴스 생성.
    implementation(project(":module-common:common-jpa"))
    implementation(libs.spring.boot.starter.jdbc)
    runtimeOnly(libs.postgresql)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
}

// Dockerfile의 COPY가 이 이름을 참조한다. version을 지정해도 jar 이름이 바뀌지 않게 고정한다.
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("app-migration.jar")
}

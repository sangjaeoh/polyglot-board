plugins {
    id("convention.common-module")
}

dependencies {
    api(project(":module-common:common-core"))
    api(libs.spring.boot.starter.data.jpa)
    api(libs.flyway.core)
    // Spring Boot Flyway 오토컨피그: 커스텀 Flyway 빈으로 migrate()를 실행하고 EMF 순서를 잡는다.
    api(libs.spring.boot.flyway)
    runtimeOnly(libs.flyway.postgresql)
}

plugins {
    id("convention.common-module")
}

dependencies {
    api(libs.spring.boot.starter.data.jpa)
    // SchemaFlywayFactory가 Flyway를 공개 시그니처로 반환한다 — api가 규칙에 부합.
    api(libs.flyway.core)
    runtimeOnly(libs.flyway.postgresql)
}

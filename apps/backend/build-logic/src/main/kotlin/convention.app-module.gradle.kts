// app-{name} 실행 모듈. Spring Boot 조립. domains·infra·common·events·query 의존 허용.
plugins {
    id("convention.java-common")
    id("org.springframework.boot")
}

enforceAllowedProjectDependencies(
    "app",
    listOf(":module-domains:", ":module-infra:", ":module-common:", ":module-events:", ":module-query:"),
)

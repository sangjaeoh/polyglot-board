plugins {
    id("convention.app-module")
}

dependencies {
    implementation(project(":module-domains:domain-board"))
    implementation(project(":module-common:common-web"))
    implementation(project(":module-common:common-jpa"))
    implementation(project(":module-common:common-core"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.springdoc.starter)
    runtimeOnly(libs.postgresql)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.webmvc.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
}

// Dockerfile의 COPY가 이 이름을 참조한다. version을 지정해도 jar 이름이 바뀌지 않게 고정한다.
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("app-api.jar")
}

// 계약 원천 openapi.json은 백엔드 루트 docs에 둔다(코드젠 입력 경로).
val openApiFile = rootProject.layout.projectDirectory.file("docs/openapi/openapi.json")

tasks.named<Test>("test") {
    // drift 게이트: 커밋된 openapi.json과 현재 계약이 다르면 실패한다.
    systemProperty("openapi.file", openApiFile.asFile.absolutePath)
}

// OpenAPI 계약을 openapi.json으로 방출한다. backend package.json의 openapi 스크립트가 위임한다.
tasks.register<Test>("generateOpenApiDocs") {
    group = "documentation"
    description = "OpenAPI 계약(openapi.json)을 방출한다"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform()
    filter { includeTestsMatching("com.board.api.openapi.OpenApiSnapshotTest") }
    systemProperty("openapi.file", openApiFile.asFile.absolutePath)
    systemProperty("openapi.write", "true")
    outputs.upToDateWhen { false }
}

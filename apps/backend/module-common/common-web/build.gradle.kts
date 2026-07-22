plugins {
    id("convention.common-module")
}

dependencies {
    api(project(":module-common:common-core"))
    api(libs.spring.boot.starter.web)
    // PaginationResponse.from(Page)가 Page를 공개 시그니처로 재노출한다 — api가 규칙에 부합.
    api(libs.spring.data.commons)
    implementation(libs.spring.boot.starter.validation)
    // @Schema(계약 문서화 애노테이션) — 런타임 소비자는 springdoc-starter를 가진 앱뿐이라 전이 부담 없음.
    implementation(libs.springdoc.common)

    testImplementation(libs.spring.boot.starter.test)
    // 부트 플러그인 없는 라이브러리 모듈은 Gradle 9가 요구하는 launcher를 직접 싣는다(버전은 BOM 소유).
    testRuntimeOnly(libs.junit.platform.launcher)
}

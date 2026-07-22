// 아키텍처 테스트 전용 모듈. 계층 규칙이 아니라 품질 게이트 플러그인만 적용한다(architecture.md).
plugins {
    id("convention.java-common")
}

dependencies {
    // 검증 대상은 모듈 목록에서 파생한다 — settings에 등록된 프로덕션 모듈 전부를 테스트 클래스패스에 올린다.
    // module-tests 계층은 의존 흐름 밖이므로 제외한다(자기 자신·미래 형제 모듈 포함).
    rootProject
        .subprojects
        .filter { it.buildFile.exists() && !it.path.startsWith(":module-tests") }
        .forEach { testImplementation(project(it.path)) }

    testImplementation(libs.archunit.junit5)
    // 규칙 코드가 직접 참조하는 타입(JpaRepository·@Entity·@Service·@Transactional)은 전이 노출에 기대지 않는다.
    testImplementation(libs.spring.boot.starter.data.jpa)
    // web 계약 표면 규칙이 참조하는 타입 — @RestController·매핑 애노테이션(spring-web), @Operation·@Schema(swagger).
    // springdoc-common은 UI 없이 swagger 애노테이션을 정합 버전으로 전이한다.
    testImplementation(libs.spring.boot.starter.web)
    testImplementation(libs.springdoc.common)
    // 규칙 픽스처 단위 테스트(JUnit Jupiter·AssertJ).
    testImplementation(libs.spring.boot.starter.test)
    // 부트 플러그인 없는 라이브러리 모듈은 Gradle 9가 요구하는 launcher를 직접 싣는다(버전은 BOM 소유).
    testRuntimeOnly(libs.junit.platform.launcher)
}

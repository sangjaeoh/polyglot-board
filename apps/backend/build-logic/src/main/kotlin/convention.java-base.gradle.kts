import com.diffplug.gradle.spotless.SpotlessExtension
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

// 전 JVM 모듈 공통: Java toolchain · Spotless · NullAway+JSpecify · Error Prone.
// 계층 플러그인이 이 플러그인을 apply 하므로 모듈은 계층 플러그인 하나로 품질 게이트를 받는다.

plugins {
    `java-library`
    id("io.spring.dependency-management")
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
}

group = "com.board"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.ver("java").toInt()))
    }
}

// 저장소는 settings의 dependencyResolutionManagement(FAIL_ON_PROJECT_REPOS)가 소유한다.

// Spring Boot BOM으로 버전리스 스타터를 해석한다(core는 스프링 의존을 선언하지 않아 순수로 남는다).
dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.ver("springBoot")}")
    }
}

dependencies {
    compileOnly(libs.lib("jspecify"))
    testCompileOnly(libs.lib("jspecify"))
    errorprone(libs.lib("errorprone-core"))
    errorprone(libs.lib("nullaway"))
}

spotless {
    java {
        palantirJavaFormat(libs.ver("palantirJavaFormat"))
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
        // JPA·프레임워크 리플렉션 대상은 생성 클래스가 없어 전 소스가 게이트 대상이다.
    }
}

tasks.withType<JavaCompile>().configureEach {
    // 정적분석(Error Prone·NullAway)은 프로덕션 소스에만 건다. 테스트 소스는 목·리플렉션 오탐이 잦다.
    options.errorprone.isEnabled.set(!name.contains("Test"))
    options.errorprone {
        disableWarningsInGeneratedCode.set(true)
        check("NullAway", CheckSeverity.ERROR)
        option("NullAway:AnnotatedPackages", "com.board")
        // JPA가 채우는 엔티티 필드는 초기화 검사에서 제외한다(매핑 애노테이션 기준).
        option(
            "NullAway:ExcludedFieldAnnotations",
            listOf(
                "jakarta.persistence.Id",
                "jakarta.persistence.Column",
                "jakarta.persistence.Enumerated",
                "jakarta.persistence.Convert",
                "jakarta.persistence.Embedded",
                "jakarta.persistence.ManyToOne",
                "jakarta.persistence.OneToOne",
                "jakarta.persistence.OneToMany",
                "jakarta.persistence.JoinColumn",
                "jakarta.persistence.Version",
                "org.springframework.beans.factory.annotation.Autowired",
            ).joinToString(","),
        )
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

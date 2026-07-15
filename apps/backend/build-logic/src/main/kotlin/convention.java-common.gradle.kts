// 금지 의존성(Lombok·H2)을 차단한다. 계층 플러그인이 이 플러그인을 apply 한다.
plugins {
    id("convention.java-base")
}

val forbidden = listOf("org.projectlombok:lombok", "com.h2database:h2")

afterEvaluate {
    val violations = configurations.flatMap { config ->
        config.dependencies.mapNotNull { dep ->
            val coord = "${dep.group}:${dep.name}"
            if (forbidden.contains(coord)) coord else null
        }
    }.distinct()

    if (violations.isNotEmpty()) {
        throw org.gradle.api.GradleException(
            "모듈 '$path'가 금지 의존성을 선언했다: $violations. " +
                "Lombok은 리뷰 불가·도구 마찰, H2는 PostgreSQL divergence로 기각됐다.",
        )
    }
}

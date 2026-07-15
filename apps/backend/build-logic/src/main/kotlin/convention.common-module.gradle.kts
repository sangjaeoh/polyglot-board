// common-{role} 모듈. core는 의존 제로, 나머지는 core 방향 단방향.
plugins {
    id("convention.java-common")
}

// 역할별 허용 의존. 새 common 모듈은 여기에 허용 집합을 등록해야 빌드된다.
val allowed = when (name) {
    "common-core" -> emptyList()
    "common-jpa", "common-web" -> listOf(":module-common:common-core")
    else -> throw GradleException("common 모듈 '$name'의 허용 의존 집합이 convention.common-module에 등록되지 않았다.")
}

enforceAllowedProjectDependencies("common", allowed)

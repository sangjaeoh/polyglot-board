import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

/**
 * 계층 컨벤션 플러그인이 선언한 프로젝트 의존이 허용 화이트리스트 안인지 설정 시점에 강제한다.
 * 선언된 모든 구성(implementation·api·compileOnly·runtimeOnly·커스텀 포함)의 프로젝트 의존을 검사해
 * 허용 밖 모듈 의존을 빌드가 막게 한다 — 구성 이름 필터로 우회 경로를 남기지 않는다.
 */
internal fun Project.enforceAllowedProjectDependencies(layer: String, allowedPathPrefixes: List<String>) {
    afterEvaluate {
        val violations = configurations
            .flatMap { config ->
                config.dependencies
                    .filterIsInstance<ProjectDependency>()
                    .map { it.path }
            }
            .distinct()
            .filter { depPath -> allowedPathPrefixes.none { depPath.startsWith(it) } }

        if (violations.isNotEmpty()) {
            throw org.gradle.api.GradleException(
                "$layer 모듈 '$path'의 계층 경계 위반: 허용되지 않은 모듈 의존 $violations. " +
                    "허용 접두: $allowedPathPrefixes",
            )
        }
    }
}

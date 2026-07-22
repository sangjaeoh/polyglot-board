pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "board-backend"

include(
    ":module-common:common-core",
    ":module-common:common-jpa",
    ":module-common:common-web",
    ":module-domains:domain-board",
    ":module-apps:app-api",
    ":module-apps:app-migration",
    ":module-tests:test-architecture",
)

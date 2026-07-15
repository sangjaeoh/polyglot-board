import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** 컨벤션 스크립트에서 버전 카탈로그(libs)에 접근한다. 버전 리터럴을 스크립트에 두지 않기 위함. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.lib(alias: String) = findLibrary(alias).get()

internal fun VersionCatalog.ver(alias: String) = findVersion(alias).get().requiredVersion

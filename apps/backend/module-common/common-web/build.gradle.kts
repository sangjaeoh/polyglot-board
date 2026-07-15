plugins {
    id("convention.common-module")
}

dependencies {
    api(project(":module-common:common-core"))
    api(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
}

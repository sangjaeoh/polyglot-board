plugins {
    `kotlin-dsl`
}

// 컨벤션 스크립트가 id로 적용하는 플러그인의 마커 jar를 build-logic 클래스패스에 올린다.
dependencies {
    implementation("org.springframework.boot:spring-boot-gradle-plugin:${libs.versions.springBoot.get()}")
    implementation("io.spring.gradle:dependency-management-plugin:${libs.versions.springDependencyManagement.get()}")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:${libs.versions.spotless.get()}")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:${libs.versions.errorpronePlugin.get()}")
}

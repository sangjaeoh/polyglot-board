plugins {
    id("convention.domain-module")
}

// JPA는 domain-module 플러그인이 제공한다. 여기선 허용 화이트리스트(common-core·jpa)만 잇는다.
dependencies {
    implementation(project(":module-common:common-core"))
    implementation(project(":module-common:common-jpa"))
}

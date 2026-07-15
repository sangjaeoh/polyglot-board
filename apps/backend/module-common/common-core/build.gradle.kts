plugins {
    id("convention.common-module")
}

// 프레임워크 의존 제로. UUIDv7 생성기만 외부 라이브러리에 의존한다.
dependencies {
    api(libs.uuid.creator)
}

plugins {
    id("convention.common-module")
}

// 프레임워크 의존 제로. UUIDv7 생성기만 외부 라이브러리에 의존한다(내부 사용만 — 공개 시그니처 미노출).
dependencies {
    implementation(libs.uuid.creator)
}

# syntax=docker/dockerfile:1
# 이 파일은 실행 앱(app-api) 루트에 있으나, 빌드 컨텍스트는 apps/backend다(Gradle 멀티모듈 전체가 필요).
# compose가 context=apps/backend, dockerfile=module-apps/app-api/Dockerfile로 지정한다. COPY 경로는 컨텍스트 기준이다.

# ---- build: Gradle bootJar (테스트는 Testcontainers Docker가 필요해 이미지 빌드에선 제외) ----
FROM amazoncorretto:25 AS build
WORKDIR /app
# gradlew는 xargs(findutils)를 필요로 하나 corretto 최소 이미지엔 없다.
RUN dnf install -y findutils && dnf clean all
COPY . .
# 캐시 마운트가 ~/.gradle(래퍼·의존성 캐시)을 빌드 간 유지한다 — COPY . . 가 레이어를 무효화해도 재다운로드하지 않는다.
RUN --mount=type=cache,target=/root/.gradle ./gradlew :module-apps:app-api:bootJar --no-daemon

# ---- runtime ----
# headless JDK(X11 없는 서버용, 풀 JDK 대비 ~100MB 절감). jlink 커스텀 런타임은 모듈 목록 유지비 대비
# 이득이 작아 기각했다(Corretto는 25용 JRE를 배포하지 않는다).
# compose의 backend healthcheck가 이미지 안의 curl에 의존한다 — AL2023 기반이라 curl-minimal이 포함된다.
# 베이스 이미지를 바꿀 땐 curl 유무를 먼저 확인한다.
FROM amazoncorretto:25-headless AS runtime
WORKDIR /app
# AL2023 이미지엔 useradd(shadow-utils)가 없어 passwd/group에 직접 기록한다.
RUN echo "appuser:x:1001:1001::/app:/sbin/nologin" >> /etc/passwd \
    && echo "appuser:x:1001:" >> /etc/group
# jar 이름은 app-api build.gradle.kts의 archiveFileName 고정에 의존한다(version 지정과 무관).
COPY --from=build /app/module-apps/app-api/build/libs/app-api.jar app.jar
EXPOSE 8080
USER appuser
# datasource는 SPRING_DATASOURCE_* 환경변수로 주입한다(compose가 postgres 서비스로 지정).
# MaxRAMPercentage: 힙 상한을 컨테이너 메모리 한도 기준으로 잡는다(기본 25%는 컨테이너에서 과소).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

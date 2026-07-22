# syntax=docker/dockerfile:1
# 이 파일은 마이그레이션 앱(app-migration) 루트에 있으나, 빌드 컨텍스트는 apps/backend다(Gradle 멀티모듈 전체가 필요).
# compose가 context=apps/backend, dockerfile=module-apps/app-migration/Dockerfile로 지정한다. COPY 경로는 컨텍스트 기준이다.

# ---- build: Gradle bootJar (테스트는 Testcontainers Docker가 필요해 이미지 빌드에선 제외) ----
FROM amazoncorretto:25 AS build
WORKDIR /app
# gradlew는 xargs(findutils)를 필요로 하나 corretto 최소 이미지엔 없다.
RUN dnf install -y findutils && dnf clean all
COPY . .
# 캐시 마운트가 ~/.gradle(래퍼·의존성 캐시)을 빌드 간 유지한다 — COPY . . 가 레이어를 무효화해도 재다운로드하지 않는다.
RUN --mount=type=cache,target=/root/.gradle ./gradlew :module-apps:app-migration:bootJar --no-daemon

# ---- runtime ----
FROM amazoncorretto:25-headless AS runtime
WORKDIR /app
# AL2023 이미지엔 useradd(shadow-utils)가 없어 passwd/group에 직접 기록한다.
RUN echo "appuser:x:1001:1001::/app:/sbin/nologin" >> /etc/passwd \
    && echo "appuser:x:1001:" >> /etc/group
# jar 이름은 app-migration build.gradle.kts의 archiveFileName 고정에 의존한다(version 지정과 무관).
COPY --from=build /app/module-apps/app-migration/build/libs/app-migration.jar app.jar
USER appuser
# 실행→migrate→종료하는 원샷 잡. datasource는 SPRING_DATASOURCE_* 환경변수로 주입한다(compose가 postgres 서비스로 지정).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

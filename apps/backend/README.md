# Board Backend

게시판 REST API 백엔드다. Spring Boot + JPA 기반의 Gradle 멀티모듈 모듈러 모놀리식으로, [폴리글랏 모노레포](../../README.md)에서 계약(OpenAPI)을 코드퍼스트로 방출하는 쪽(originator)을 맡는다.

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791)
![Gradle](https://img.shields.io/badge/Gradle-9.5-02303A)
![Architecture](https://img.shields.io/badge/Modular%20Monolith-multi--module-blue)

위 배지는 표시용이다. 버전 정본은 [`gradle/libs.versions.toml`](gradle/libs.versions.toml)이 소유한다.

## REST API

베이스 경로는 `/api/v1/posts`다. 실행 후 Swagger UI(http://localhost:8080/swagger-ui.html)에서 확인할 수 있다.

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/v1/posts` | 글 목록 (페이지네이션 · 최신순 · 활성 글만) |
| GET | `/api/v1/posts/{id}` | 글 상세 |
| POST | `/api/v1/posts` | 글 작성 |
| PUT | `/api/v1/posts/{id}` | 글 수정 |
| DELETE | `/api/v1/posts/{id}` | 글 삭제 (소프트 삭제) |

에러 응답은 RFC 9457 ProblemDetail이다. 확장 멤버로 기계 분기용 `code`, 필드 검증 오류 `errors[]`, 관측용 `traceId`를 싣는다.

## 실행하기

JDK 25가 필요하다(모노레포 루트의 `mise install`로 맞출 수 있다). 테스트는 Testcontainers를 쓰므로 Docker도 필요하다.

PostgreSQL을 먼저 띄운다. 모노레포 루트의 compose를 쓰면 호스트 5433에 노출된다.

```bash
docker compose up -d postgres   # 모노레포 루트에서
```

애플리케이션을 실행한다. `application.yml` 기본 datasource는 `localhost:5432`이므로 compose postgres(5433)를 쓸 때는 URL을 오버라이드한다.

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/boardapi \
  ./gradlew :module-apps:app-api:bootRun
```

스키마는 기동 시 Flyway가 생성·마이그레이션한다(`ddl-auto: validate`). 프론트엔드까지 포함한 전체 스택 실행은 [루트 README](../../README.md)의 Docker Compose 절을 따른다.

## 모듈 구조

의존은 `app → domain → common` 한 방향으로만 흐른다. 경계는 리뷰가 아니라 아키텍처 테스트(ArchUnit)와 컨벤션 플러그인이 강제한다.

```
backend/
├── build-logic/                 Gradle 컨벤션 플러그인(계층 배선·품질 게이트)
├── module-apps/
│   └── app-api/                 실행 앱
├── module-domains/
│   └── domain-board/            게시판 도메인
├── module-common/
│   ├── common-core/             에러 베이스(ErrorCode·BaseException)·UUIDv7 생성기
│   ├── common-jpa/              BaseTimeEntity·JPA Auditing·스키마별 Flyway 배선
│   └── common-web/              GlobalExceptionHandler(ProblemDetail)·필드 에러
└── docs/
    ├── openapi/openapi.json     방출된 계약
    └── *.md                     개발 규칙 문서
```

- 도메인 서비스는 역할별로 쪼갠다: `PostReader` · `PostAppender` · `PostModifier` · `PostRemover`.
- 엔티티 ID는 UUIDv7, 삭제는 `deleted_at` 소프트 삭제, 생성·수정 시각은 JPA Auditing이 채운다.

## OpenAPI 계약 방출

이 앱이 모노레포 계약의 유일 원천이다. springdoc으로 코드에서 방출하고, 방출물은 커밋한다.

```bash
./gradlew :module-apps:app-api:generateOpenApiDocs   # → docs/openapi/openapi.json
```

- 코드와 커밋된 계약의 불일치는 스냅샷 테스트가 잡는다(`./gradlew build`에 포함).
- 계약의 하류 소비(orval 코드젠 → 프론트엔드)는 루트가 배선한다 → [`docs/sharing.md`](../../docs/sharing.md).

## 빌드와 검증

```bash
./gradlew build
```

한 명령에 품질 게이트가 모두 포함된다.

- Spotless (Palantir Java Format) — 포맷
- Error Prone + NullAway (JSpecify) — 정적분석·널 안전성
- ArchUnit — 모듈·패키지 경계 테스트
- 단위·웹·통합 테스트 — DB는 Testcontainers(PostgreSQL)로 뜬다(Docker 필요)
- OpenAPI 스냅샷 테스트 — 코드↔커밋 계약 일치

## 기술 스택

| 범주 | 채택 |
| --- | --- |
| 언어·런타임 | Java 25 |
| 프레임워크 | Spring Boot 4.1 |
| 웹 | Spring MVC + 가상 스레드 |
| 영속 | Spring Data JPA |
| DB | PostgreSQL 17 |
| 마이그레이션 | Flyway |
| API 문서·계약 | springdoc (코드퍼스트 OpenAPI 방출) |
| 빌드 | Gradle 9.5 (Kotlin DSL · 버전 카탈로그 · build-logic 컨벤션 플러그인) |
| 포맷·정적분석 | Spotless (Palantir Java Format) · NullAway/JSpecify · Error Prone |
| 경계 강제 | 컨벤션 플러그인 · ArchUnit |
| 테스트 DB | Testcontainers |

## 개발 문서

개발 규칙은 `docs/`의 네 문서가 소유한다. 진입 앵커는 [`AGENTS.md`](AGENTS.md)다.

- [`docs/architecture.md`](docs/architecture.md) — 모듈 구조·패키지 구조·의존 방향·리포지토리 접근 범위
- [`docs/coding-conventions.md`](docs/coding-conventions.md) — 타입 선언·객체 생성/변환·접근제한자·네이밍·주석
- [`docs/entity-persistence.md`](docs/entity-persistence.md) — 엔티티 ID·버저닝·물리 FK 금지·연관·상태 전이
- [`docs/code-quality.md`](docs/code-quality.md) — Spotless·NullAway·Error Prone 게이트와 도구 버전

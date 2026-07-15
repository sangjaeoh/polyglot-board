# Polyglot Monorepo Board

폴리글랏 모노레포 가이드를 기반으로 구현한 게시판 데모 프로젝트다. 서로 다른 언어의 두 애플리케이션 — Java/Spring Boot 백엔드와 TypeScript/Next.js 프론트엔드 — 이 한 저장소에서 함께 빌드되고, 하나의 계약(OpenAPI)으로 이어지는 구조가 실제로 동작하는지 확인한다.

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F)
![Next.js](https://img.shields.io/badge/Next.js-16-black)
![React](https://img.shields.io/badge/React-19.2-61DAFB)
![TypeScript](https://img.shields.io/badge/TypeScript-strict-3178C6)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791)
![Turborepo](https://img.shields.io/badge/Turborepo-pnpm%20workspaces-EF4444)

게시판이 목록·상세·작성·수정·삭제까지 끝까지 도는 것 자체가, 백엔드와 프론트엔드가 계약으로 제대로 연결됐다는 증거다.

## 기능

- 글 목록(최신순) · 글 상세 · 작성 · 수정 · 삭제(소프트 삭제)
- 제목 200자 · 내용 10,000자 제한
- 운영체제 테마에 따른 라이트/다크 모드
- 첫 실행 시 예시 글을 넣어 주는 데모 시드

## 빠른 시작 (Docker Compose)

Docker만 있으면 한 번에 실행된다.

```bash
git clone <this-repo>
cd polyglot-board
docker compose up --build
```

postgres(스키마는 Flyway가 생성) → backend → seed(예시 글) → frontend 순으로 healthcheck 게이트를 거쳐 기동한다.

| 서비스 | URL |
| --- | --- |
| 게시판 (Next.js) | http://localhost:3000 |
| REST API (Spring Boot) | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| PostgreSQL | localhost:5433 (`boardapi` / `boardapi`) |

이것저것 시도하다 처음 상태로 되돌리려면 볼륨까지 지우고 다시 올린다.

```bash
docker compose down -v && docker compose up --build
```

## 아키텍처

루트는 얇은 교차언어 허브다. 언어별 규칙·구조는 각 서브 프로젝트가 자립적으로 소유하고, 루트는 계약·워크스페이스·태스크 그래프만 소유한다.

```
polyglot-board/
├── apps/
│   ├── backend/                         Spring Boot — Gradle 멀티모듈 모듈러 모놀리식
│   │   ├── module-apps/app-api          실행 앱
│   │   ├── module-domains/domain-board  게시판 도메인
│   │   ├── module-common/               Java 내부 공유
│   │   └── docs/openapi/openapi.json    방출된 계약
│   └── frontend/                        Next.js — 자체 Turborepo 서브트리(FSD-lite)
│       ├── apps/web                     게시판 웹 앱
│       └── packages/                    TS 내부 공유
├── packages/
│   └── shared-types/                    계약에서 생성한 타입·Zod·API client
├── docs/
├── docker-compose.yml      
└── turbo.json  pnpm-workspace.yaml  .mise.toml
```

### 계약(OpenAPI) 파이프라인

언어를 넘는 "공유 타입"은 없다. 계약의 유일 원천은 백엔드가 코드퍼스트로 방출하는 OpenAPI 문서이고, 프론트엔드는 그 계약에서 생성된 산출물만 소비한다. 방향은 단방향이다.

```mermaid
flowchart LR
    A["backend (app-api)<br/>springdoc 코드퍼스트 방출"] -->|openapi.json| B["shared-types<br/>orval 코드젠: 타입·Zod·client"]
    B --> C["@board/api-client<br/>(server-only 경계)"]
    C --> D["web (Next.js)<br/>RSC · Server Actions"]
```

- Turborepo 태스크 그래프가 순서를 강제한다: `backend#openapi → shared-types#codegen → (프론트) build`.
- 생성물은 커밋한다. 계약↔생성물 불일치는 `pnpm verify`의 drift 게이트가 잡는다.
- 에러 계약은 RFC 9457 ProblemDetail이며, 공용 에러 스키마는 `shared-types`가 하나만 소유한다.

## 기술 스택

| 영역 | 채택 |
| --- | --- |
| 백엔드 | Java 25 · Spring Boot 4.1 · Spring MVC(가상 스레드) · Spring Data JPA/QueryDSL · Flyway · Gradle 9.5 멀티모듈 |
| 프론트엔드 | Node.js 24 · Next.js 16 · React 19.2 · TypeScript · Tailwind CSS v4 · FSD-lite |
| 데이터베이스 | PostgreSQL 17 |
| 계약·코드젠 | OpenAPI(springdoc) · orval · Zod |
| 모노레포 | Turborepo · pnpm workspaces · mise |
| 품질 게이트 | Spotless·NullAway·Error Prone·ArchUnit(백엔드) / ESLint·dependency-cruiser·`server-only` 경계(프론트) · drift 게이트(루트) |

## 로컬 개발

### 요구사항

툴체인 버전 정본은 [`.mise.toml`](.mise.toml)이 소유한다. [mise](https://mise.jdx.dev)를 쓰면 한 번에 맞춰진다.

```bash
mise install   # JDK(corretto-25) · Node 24 · pnpm 9
```

### 데이터베이스

로컬 5432 충돌을 피해 compose의 postgres는 호스트 5433에 노출된다.

```bash
docker compose up -d postgres
```

### 백엔드

`application.yml` 기본값은 `localhost:5432`이므로, compose postgres(5433)를 쓸 때는 URL을 오버라이드한다.

```bash
cd apps/backend
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/boardapi \
  ./gradlew :module-apps:app-api:bootRun
```

### 프론트엔드

```bash
pnpm install
pnpm dev       # turbo가 계약 방출 → 코드젠 → next dev 순으로 실행한다
```

개발 모드에서 `BACKEND_API_URL`은 `http://localhost:8080`이 기본값이다. http://localhost:3000 에서 확인한다.

### 검증

```bash
pnpm verify
```

한 명령으로 전체 게이트가 돈다.

- `turbo run verify` — 백엔드 `./gradlew build`(테스트·아키텍처 테스트·정적분석 포함, Testcontainers로 PostgreSQL 필요), 프론트 lint·typecheck·format 체크
- `depcruise` — 프론트 패키지 의존 방향 검사
- `drift:check` — 계약에서 재생성한 결과와 커밋된 생성물의 일치 검사

## 문서

아키텍처·공유 규칙은 문서가 소유한다. 진입 앵커는 [`AGENTS.md`](AGENTS.md)다.

- [`docs/architecture.md`](docs/architecture.md) — 저장소 구조 · 공유 패키지 배치 · 소유/위임 경계 · 경계 강제
- [`docs/sharing.md`](docs/sharing.md) — 계약 seam · 코드젠 파이프라인 · drift 게이트 · 에러 모델
- [`apps/backend/README.md`](apps/backend/README.md) — 백엔드(Spring Boot) — REST API·모듈 구조·실행·검증
- [`apps/frontend/README.md`](apps/frontend/README.md) — 프론트엔드(Next.js) — 화면·구조·데이터 흐름·검증

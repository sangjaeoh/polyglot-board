# Architecture

## 언제

- 모듈·앱·루트 패키지 레이어·워크스페이스 topology를 추가·이동할 때.
- 모듈 경계·의존 방향·소유(변경 표면 4분류)를 정할 때.
- 새 클래스·타입·공유물을 세 공유 레벨 중 배치할 때.

## 규칙

### 모듈 지도

- 루트는 얇은 교차언어 허브다. `docs/`에는 구조·공유 규칙만 둔다.
- 언어별 규칙은 각 서브가이드가 소유한다.
- `apps/backend`는 Spring Boot 서브가이드다. `AGENTS.md`·`docs/`·Gradle 멀티모듈·`build-logic/`을 소유한다.
- `apps/frontend`는 Next.js 서브가이드다. `AGENTS.md`·`docs/`·Turborepo 서브트리(`apps/*`, `packages/*`)를 소유한다.
- 프론트 standalone의 `pnpm-workspace.yaml`·`turbo.json`은 통합 시 루트 설정으로 흡수한다.
- 루트 워크스페이스·turbo 설정은 하나만 유효하다.
- 의존 방향은 단방향이다.
  - TS 앱 → 루트/프론트 `packages`
  - 역방향·앱 간 의존 금지

구조:
```text
{repo}/
├── AGENTS.md
├── CLAUDE.md
├── docs/
│   ├── architecture.md
│   └── sharing.md
├── apps/
│   ├── backend/
│   └── frontend/
├── packages/
│   └── shared-types/
├── turbo.json
├── pnpm-workspace.yaml
└── .mise.toml
```

### 패키지·워크스페이스 구조

- 공유는 세 레벨로 구분한다.

| 레벨 | 위치 | 대상 |
|---|---|---|
| 폴리글랏 경계 | 루트 `packages/*` | 계약 생성물 |
| 프론트 내부 | `apps/frontend/packages/*` | TS 공유 |
| 백엔드 내부 | `apps/backend/module-common/*` | Java 공유 |

- 세 레벨 모두 도메인 로직·도메인 지식을 포함하지 않는다.
- 언어를 넘는 공유 타입은 만들지 않는다.
- 루트 `packages/shared-types`는 계약에서 생성된 산출물만 포함한다.
- 내부 공유 방향은 검사로 강제한다.

- `pnpm-workspace.yaml`이 워크스페이스 멤버를 정의한다.
- 프론트 서브트리는 루트 워크스페이스로 평탄화한다.
- 백엔드는 디렉토리 단위 멤버로 편입한다.

멤버:

| 경로 |
|---|
| `apps/backend` |
| `apps/frontend/apps/*` |
| `apps/frontend/packages/*` |
| `packages/*` |

- `apps/backend/package.json`은 JS 워크스페이스 편입용이다.
- 툴체인 버전 정본은 `.mise.toml`과 루트 `package.json`의 `packageManager`가 소유한다.

### 소유·위임 경계

- 작업 변경 표면을 먼저 4분류하고 소유를 결정한다.

| 변경 표면 | 예 | 소유 |
|---|---|---|
| backend-only | `apps/backend/**` 언어 코드 | `apps/backend` 서브가이드 |
| frontend-only | `apps/frontend/**` 언어 코드 | `apps/frontend` 서브가이드 |
| cross-language(seam) | OpenAPI 계약·`packages/shared-types`·양쪽 변경 | 루트 |
| root-infra | `turbo.json`·`pnpm-workspace.yaml`·`.mise.toml`·루트 `package.json`·`apps/backend/package.json` | 루트 |

- `apps/backend/package.json`은 backend 코드가 아닌 root-infra다.
- 프론트 `package.json`의 의존성·스크립트는 frontend-only다.
- 루트 `packages/*` 신설·이동은 topology 변경이므로 architecture 결정이다.
- 프론트 내부 패키지 신설은 frontend-only다.
- 계약을 변경하면 단일언어 변경이어도 cross-language로 승격한다.
- root-infra와 언어 영역을 함께 변경하면 루트는 infra, 서브가이드는 언어 영역을 소유한다.
- 루트는 실행 순서·게이트만 제공하고, 계약 필드·비즈니스 규칙·API 정책을 정의하지 않는다.
- 계약 필드·비즈니스 규칙·API 정책은 생성 주체가 소유한다.
- 계약 방출·소비 방향은 sharing 규칙을 따른다.
- `AGENTS.md`는 루트와 두 서브가이드로 구성한다.
- 서브가이드는 standalone으로 유효하다.

### 경계 강제

- 경계는 리뷰가 아닌 검사로 강제한다.
- 규칙 본문은 각 소유 문서가 관리하고, architecture는 강제 장치만 정의한다.

| 경계 | 강제 장치 |
|---|---|
| 모듈·패키지 의존 방향 | dependency-cruiser |
| 백엔드 Java 모듈 경계·내부 공유 방향 | ArchUnit |
| 계약·생성물 일치 | drift 게이트 |
| 계약 파괴적 변경 | oasdiff |
# Architecture

## 언제

- 모듈·앱·루트 패키지 레이어·워크스페이스 topology를 추가·이동할 때.
- 모듈 경계·의존 방향·소유(변경 표면 4분류)를 정할 때.
- 새 클래스/타입·공유물을 세 공유 레벨 중 어디에 둘지 판별할 때. 계약·생성 타입의 공유방법은 → [sharing](sharing.md).

## 규칙

### 모듈 지도

- 루트는 얇은 교차언어 허브다. `docs/`에 교차언어 규칙(구조·공유)만 두고 언어별 규칙은 서브가이드가 소유한다.
  - 얇은 루트 + 자립 서브를 택했다. 통합 병합·미니멀 이정표를 기각했다(서브가이드 standalone 유효성은 → 소유·위임 경계).
- `apps/backend`는 Spring Boot 서브가이드다. 자기 `AGENTS.md`·`docs/`·Gradle 멀티모듈·`build-logic/`을 소유한다.
- `apps/frontend`는 Next.js 서브가이드다. 자기 `AGENTS.md`·`docs/`와 자체 Turborepo 서브트리(`apps/*`·`packages/*`)를 소유한다. 내부 구조는 프론트 가이드가 소유한다.
- 프론트가 standalone에서 갖는 자체 `pnpm-workspace.yaml`·`turbo.json`은 임베드되면 루트 것에 흡수된다 — 루트 워크스페이스·turbo 하나만 유효하다.
- 의존은 한 방향으로만 흐른다: TS 앱 → 루트/프론트 `packages`. 역방향·앱 간 의존은 금지다(→ 경계 강제).

```
{repo}/
├── AGENTS.md              루트 앵커(트리아지·2문서 인덱스·자기검증)
├── CLAUDE.md
├── docs/
│   ├── architecture.md    이 문서 — 구조·배치·소유
│   └── sharing.md         공유방법 — 계약·코드젠·에러
├── apps/
│   ├── backend/           Spring Boot 서브가이드 + Gradle 멀티모듈 + package.json(워크스페이스 멤버)
│   └── frontend/          Next.js 서브가이드 + 자체 Turborepo(apps/*·packages/*)
├── packages/
│   └── shared-types/      계약(OpenAPI)에서 생성한 타입·Zod·client (→ sharing)
├── turbo.json  pnpm-workspace.yaml  .mise.toml
```

### 공유 패키지 배치

- 공유는 세 레벨로 나뉜다. 혼동하지 않는다.

  | 레벨          | 위치                        | 담는 것                          |
  | ------------- | --------------------------- | -------------------------------- |
  | 폴리글랏 경계 | 루트 `packages/*`           | `shared-types`(계약 생성물)      |
  | 프론트 내부   | `apps/frontend/packages/*`  | TS 공유(ui·api-client 등)        |
  | 백엔드 내부   | `apps/backend/module-common/*` | Java 공유(Gradle)             |

- 세 레벨 모두 도메인 로직·도메인 지식을 담지 않는다 — 기술 지원만 둔다.
- 언어를 넘는 "공유 타입"은 없다. 루트 `packages/shared-types`는 계약에서 생성된 산출물을 담고 TS 앱이 소비한다. 정체·생성·진화는 → [sharing](sharing.md).
- 내부 공유 방향(백엔드·프론트 각각)은 검사가 강제한다(→ 경계 강제).

### 워크스페이스 골격

- `pnpm-workspace.yaml`이 무엇을 한 워크스페이스로 묶는지 정한다. 프론트 서브트리(`apps/*`·`packages/*`)와 백엔드·루트 공유를 함께 glob 한다 — 프론트 것도 둘 다 루트로 평탄화되고 백엔드는 디렉토리 통째 멤버다.

```
packages:
  - apps/backend
  - apps/frontend/apps/*
  - apps/frontend/packages/*
  - packages/*
```

- 백엔드는 얇은 `apps/backend/package.json`으로 JS 워크스페이스에 편입한다. 멤버십을 위한 것이고, 그 스크립트·태스크 그래프 배선은 → [sharing](sharing.md).
- 툴체인 버전 정본은 `.mise.toml`(JDK·Node·pnpm)과 루트 `package.json`의 `packageManager`가 소유한다.

### 소유·위임 경계

- 작업의 변경 표면을 먼저 4분류하고 소유를 가른다.

  | 변경 표면            | 예                                                        | 소유                        |
  | -------------------- | --------------------------------------------------------- | --------------------------- |
  | backend-only         | `apps/backend/**` 언어 코드(Gradle 모듈·소스)             | `apps/backend` 서브가이드   |
  | frontend-only        | `apps/frontend/**` 언어 코드(프론트 자체 apps·packages 포함) | `apps/frontend` 서브가이드 |
  | cross-language(seam) | OpenAPI 계약·`packages/shared-types`·양쪽 동시             | 루트(→ [sharing](sharing.md)) |
  | root-infra           | `turbo.json`·`pnpm-workspace.yaml`·`.mise.toml`·루트 `package.json`·`apps/backend/package.json` | 루트 |

- `apps/backend/package.json`은 backend 코드가 아니라 root-infra다. 프론트 `package.json`의 의존성·스크립트는 frontend-only다.
- 루트 `packages/*` 신설·이동은 topology 변경이라 architecture 결정이다. 프론트 내부 패키지 신설은 frontend-only다.
- 단일언어로 보여도 계약(seam)을 바꾸면 cross-language로 승격한다.
- 한 변경이 root-infra와 언어 스코프를 함께 건드리면 루트가 infra 부분을 소유하고 언어 부분은 서브가이드에 위임한다.
- 루트는 순서·게이트만 제공하고 계약 필드·비즈니스 규칙·API 정책을 originate 하지 않는다. 계약은 백엔드가, 소비는 프론트가 소유한다(방출·소비 방향은 → [sharing](sharing.md)의 계약 seam).
- 페더레이션 토폴로지: `AGENTS.md`는 셋이다(루트 + 서브 둘). 서브가이드는 standalone으로도 유효하다.

### 경계 강제

- 아래는 리뷰가 아니라 검사가 막는 경계다. 규칙 본문은 각 소유 문서가 갖고, 여기선 강제 장치만 대조한다.

  | 경계                                                | 강제 장치                        |
  | --------------------------------------------------- | -------------------------------- |
  | 모듈/패키지 의존 방향                               | dependency-cruiser            |
  | 백엔드 Java 모듈 경계·내부 공유 방향                | ArchUnit                         |
  | 계약↔생성물 일치                                    | drift 게이트(→ [sharing](sharing.md)) |

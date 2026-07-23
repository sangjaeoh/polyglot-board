# Board Frontend

게시판 웹 프론트엔드다. Next.js App Router 기반의 서버 우선 구조로, [폴리글랏 모노레포](../../README.md)에서 백엔드가 방출한 계약(OpenAPI)의 생성물을 소비하는 쪽을 맡는다. 자체 서브트리(`apps/*`·`packages/*`)를 FSD-lite 규율로 나눈다.

![Node](https://img.shields.io/badge/Node-24_LTS-339933)
![Next.js](https://img.shields.io/badge/Next.js-16-black)
![React](https://img.shields.io/badge/React-19.2-61DAFB)
![TypeScript](https://img.shields.io/badge/TypeScript-strict-3178C6)
![Tailwind](https://img.shields.io/badge/Tailwind_CSS-v4-38BDF8)

위 배지는 표시용이다. 버전 정본은 각 패키지의 `package.json`·`pnpm-lock.yaml`이 소유한다.

## 화면

| 경로 | 화면 |
| --- | --- |
| `/` | 글 목록 (페이지네이션 · 최신순) |
| `/posts/new` | 글 작성 |
| `/posts/[id]` | 글 상세 |
| `/posts/[id]/edit` | 글 수정 |

- 운영체제 테마에 따라 라이트/다크 모드가 자동 적용된다.

## 실행하기

pnpm 워크스페이스와 태스크 그래프는 모노레포 루트 하나만 유효하므로 루트에서 실행한다. Node 24·pnpm 9가 필요하고(루트의 `mise install`), 개발 태스크가 계약 방출(`backend#openapi`)을 선행하므로 JDK 25도 필요하다.

```bash
pnpm install       # 모노레포 루트에서
pnpm dev           # 계약 방출 → 코드젠 → next dev 순으로 실행된다
```

- 백엔드 API가 떠 있어야 데이터가 나온다 → [백엔드 README](../backend/README.md)의 실행하기.
- 백엔드 주소는 서버 전용 env `BACKEND_API_URL`로 읽는다. 개발 기본값은 `http://localhost:8080`이고, 부팅 시 Zod로 검증한다.
- http://localhost:3000 에서 확인한다. 전체 스택 한 번에 실행은 [루트 README](../../README.md)의 Docker Compose 절을 따른다.

## 구조

의존은 `앱 → 내부 packages → (루트) shared-types` 한 방향으로만 흐른다. 경계는 dependency-cruiser와 ESLint(boundaries)가 강제한다.

```
frontend/
├── apps/
│   └── web/                     게시판 웹 앱
│       └── src/
│           ├── app/             라우트·레이아웃·글로벌 스타일
│           ├── features/board/  목록·폼·Server Actions·서버 페치
│           └── entities/post/   Post 뷰모델
├── packages/
│   ├── api-client/              server-only API 클라이언트 — 계약 생성물을 벤더 중립 경계로 감싼다
│   ├── ui/                      공용 UI 컴포넌트
│   └── config/                  env(Zod 검증)·상수 — 서버 전용 진입점 분리
└── docs/                        개발 규칙 문서
```

## 데이터 흐름

- 읽기는 RSC 서버 페치다. 페이지가 `api-client`를 통해 백엔드를 직접 호출하고, 클라이언트 번들에는 내부 URL이 실리지 않는다(`server-only` 가드).
- 쓰기는 Server Actions다. 폼 입력(ingress)을 서버에서 Zod로 다시 검증하고, 성공 시 `updateTag`로 캐시를 무효화한 뒤 리다이렉트한다.
- 백엔드 에러(RFC 9457 ProblemDetail)는 `ApiError`로 감싸 폼 필드 에러로 매핑한다. 예상 밖 에러는 에러 바운더리로 넘긴다.
- 타입·Zod 스키마는 루트 `shared-types`가 계약에서 생성한 것만 쓴다. 수기 타입을 두지 않는다 → [`docs/sharing.md`](../../docs/sharing.md).

## 검증

모노레포 루트에서 전체 게이트를 돌린다.

```bash
pnpm verify        # lint · typecheck · format 체크 · depcruise · drift 체크 포함
```

- ESLint — boundaries(FSD-lite 레이어)·jsx-a11y·react-hooks
- dependency-cruiser — 패키지 의존 방향
- TypeScript strict — 타입 체크
- Prettier — 포맷 체크

## 기술 스택

| 범주 | 채택 |
| --- | --- |
| 런타임 | Node.js 24 (LTS) |
| 프레임워크 | Next.js 16 |
| UI | React 19.2 |
| 언어 | TypeScript (strict) |
| 모노레포 | Turborepo + pnpm workspaces |
| 내부 구조 | Feature-Sliced Design |
| 검증 | Zod |
| 스타일 | Tailwind CSS v4 |
| 경계 강제 | dependency-cruiser · eslint-plugin-boundaries · `server-only` |

## 개발 문서

개발 규칙은 `docs/`의 다섯 문서가 소유한다. 진입 앵커는 [`AGENTS.md`](AGENTS.md)다.

- [`docs/architecture.md`](docs/architecture.md) — 워크스페이스·패키지 구조·FSD-lite 레이어·의존 방향·서버/클라 경계·시크릿 경계
- [`docs/coding-conventions.md`](docs/coding-conventions.md) — 네이밍·타입 선언·컴포넌트/스타일 컨벤션·접근성
- [`docs/rendering.md`](docs/rendering.md) — 렌더 경계 판단·전략·라우팅·async 상태·메타데이터
- [`docs/data.md`](docs/data.md) — read-model·데이터 흐름·BFF·api-client·캐시·응답 계약·폼
- [`docs/code-quality.md`](docs/code-quality.md) — 포맷·린트·타입·경계 게이트와 도구 버전

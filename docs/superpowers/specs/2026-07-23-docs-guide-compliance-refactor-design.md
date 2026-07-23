# docs 가이드 정합 리팩토링 — 1차 배치 설계 (2026-07-23)

## 배경

`docs/superpowers/reports/2026-07-23-docs-compliance-audit.md`가 루트·backend·frontend 가이드 15개 문서 대비 위반 15건, 불확실 13건을 발견했다. 전부 한 번에 다루기엔 성격이 너무 달라(설정 한 줄 ~ 테스트 인프라 신축) 우선순위 4건만 1차 배치로 먼저 진행한다. 나머지 11건 위반·13건 불확실 항목은 이번 스펙의 범위 밖이며 별도 배치로 다룬다.

## 범위 (1차 배치 4건)

| ID | 위반 | 근거 문서 |
|---|---|---|
| A | backend 상관 ID(`RequestIdFilter`) 미구현 | `apps/backend/docs/observability.md:34-38` |
| B | 루트 `packages/shared-types`에 수기 로직 혼입 + ProblemDetail Zod "생성" 미충족 | `docs/architecture.md:56`, `docs/sharing.md:113` |
| C | frontend 캐시 무효화가 `revalidatePath`로만 구현, 문서 규정(`cacheTag`/`revalidateTag`/`updateTag`) 미충족 | `apps/frontend/docs/data.md` 캐시 절 |
| D | frontend 테스트 인프라·테스트 파일 전무 | `apps/frontend/docs/testing.md` |

4건은 서로 다른 언어·서브트리라 독립적이다. 구현 단계에서 병렬 진행 가능.

## A. backend — RequestIdFilter

### 설계

- 신규 클래스 `com.board.common.web.correlation.RequestIdFilter` (`common-web` 모듈, `OncePerRequestFilter` 상속, `@Component`).
  - `BoardApiApplication`이 `@SpringBootApplication(scanBasePackages = "com.board")`로 전체 스캔하므로 별도 등록 코드 불필요 — Spring Boot가 `Filter` 빈을 자동 등록한다.
- 동작:
  1. `X-Request-Id` 요청 헤더가 있으면 그 값을 사용, 없으면 `UUID.randomUUID().toString()`으로 생성.
  2. 응답 헤더 `X-Request-Id`에 반환.
  3. `MDC.put("requestId", id)`.
  4. `try { chain.doFilter(...) } finally { MDC.remove("requestId") }`.
- `GlobalExceptionHandler.java`: `MDC.get("traceId")` → `MDC.get("requestId")`로 변경(1줄). 응답 필드명 `traceId`는 계약(ProblemDetail 확장 멤버, `docs/sharing.md:119-121`)이므로 그대로 유지 — 내부 MDC 키 이름과 외부 응답 필드명은 별개 개념.
- 신규 패키지이므로 `package-info.java`에 `@NullMarked` 추가(기존 패키지 컨벤션).

### 테스트

- `RequestIdFilterTest`: 헤더 수용/생성/응답 헤더 반영/MDC 제거(finally) 검증.
- 기존 `BoardControllerIntegrationTest`(또는 신규 케이스)에 `X-Request-Id` 요청 시 에러 응답의 `traceId` 필드가 요청 헤더 값과 일치하는지 검증 추가.

### 범위 밖

- 비요청 경로(이벤트 소비·배치)의 `eventId`/`jobId` MDC — 해당 코드(이벤트 소비·배치 모듈) 자체가 아직 없어 검증 대상 없음(`observability.md:39-41`).
- 실행자 간 MDC 전파용 `TaskDecorator` — 비동기 실행자 사용처가 현재 없어 불필요(YAGNI).

## B. 루트 — shared-types 생성물 원칙 정합

### 제약 조건 (조사로 확정)

- `packages/api-client`(패키지)는 `apps/frontend/apps/web`(앱)을 import할 수 없다(`no-packages-to-apps` dependency-cruiser 규칙).
- `api-client/src/client.ts`는 `import 'server-only'`를 갖는다. 이 파일과 연결된 모듈이 클라 번들에 포함되면 빌드 실패한다(poison import).
- `apps/web/src/entities/post/model.ts`는 클라 컴포넌트(`PostTime.tsx`)에서도 소비되므로, 여기서의 **값(value) import**는 poison 전파 위험이 있다. **타입(type) import**는 컴파일 타임에 지워지므로 안전하다.
- `@orval/zod`(설치 버전 7.21.0)의 응답 스키마 추출 함수(`parseBodyAndResponse`, `dist/index.js:430`)는 `content["application/json"]`과 `content["multipart/form-data"]`만 하드코딩 인식한다. `override.contentType.include/exclude` 설정은 TS/axios 생성기(`getResponse`)에만 반영되고 zod 생성기는 무시한다 — 따라서 `application/problem+json`으로 선언된 `ProblemDetail` 응답은 설정 변경만으로는 orval이 생성할 수 없다(소스 확인 완료).

### 결정: ProblemDetail 생성 방식

`architecture.md:56`("생성물만 포함")과 `sharing.md:113`("shared-types가 생성해 소유")를 동시에 충족하는 유일한 방법은 orval과 별도로 이 스키마 하나만 생성하는 전용 스크립트다. 범용 JSON-schema-to-zod 변환기가 아니라 `ProblemDetail` 컴포넌트 전용 최소 매퍼로 한정한다(YAGNI).

### 설계

- `packages/shared-types/scripts/generate-problem-detail.mjs` 신설.
  - `apps/backend/docs/openapi/openapi.json`의 `components.schemas.ProblemDetail`을 읽어 필드(`type`/`title`/`status`/`detail`/`instance`/`code`/`errors`/`traceId`)와 `required` 목록을 기준으로 zod 스키마를 `src/generated/problem-detail.ts`에 방출.
  - `package.json`의 `codegen` 스크립트에서 orval 실행 다음 순서로 연결.
- `packages/shared-types/src/index.ts`:
  - 제거: `postIdSchema`, `postResponseSchema`/`postPageResponseSchema`의 `.extend()` 브랜드 합성, 수기 `problemDetailSchema`.
  - 유지: `postCreateRequestSchema`/`postUpdateRequestSchema`/`postListQuerySchema`(단순 리네임 재노출 — 위반 아니었음, 변경 없음). `postResponseSchema`/`postPageResponseSchema`는 브랜드 없는 raw 재노출(`getPostResponse`/`listPostsResponse` 그대로)로 유지 — api-client가 여기서 가져와 브랜드를 합성한다.
  - 추가: `export * from './generated/problem-detail'`.
- 신규 `apps/frontend/packages/api-client/src/schemas.ts`:
  - `postIdSchema = z.string().uuid().brand<'PostId'>()`.
  - `postResponseSchema`/`postPageResponseSchema` — `shared-types`의 raw 버전을 `.extend({id: postIdSchema, ...})`로 브랜드 합성(기존 로직 그대로 이전).
  - `PostId`/`PostResponse`/`PostPageResponse`/`PostSummary` 타입 재노출.
- `apps/frontend/packages/api-client/src/index.ts`: `schemas.ts` re-export 추가.
- `apps/frontend/packages/api-client/src/client.ts`: `postIdSchema`/`postResponseSchema`/`postPageResponseSchema`/관련 타입의 import 소스를 `'shared-types'` → `'./schemas'`로 교체. 로직 변경 없음. `problemDetailSchema`/`PostCreateRequest`/`PostUpdateRequest`는 `'shared-types'`에서 그대로(변경 없음).
- `apps/frontend/packages/api-client/src/error.ts`: 변경 없음(`problemDetailSchema` 경로 불변).
- `apps/frontend/apps/web/src/features/board/index.server.ts`(서버 전용 배럴): `postIdSchema` 값 import·`PostResponse` 타입 import를 `@/entities/post` 대신 `@board/api-client`에서 직접.
- `apps/frontend/apps/web/src/entities/post/model.ts`:
  - `postIdSchema`/`postResponseSchema`/`postPageResponseSchema` **값** re-export 제거.
  - `PostId`/`PostResponse`/`PostPageResponse`/`PostSummary`는 `import type {...} from '@board/api-client'`로 **타입 전용** re-export 유지.
  - `formatDateTime`은 변경 없음.
- `apps/frontend/apps/web/src/entities/post/index.ts`: model.ts 변경에 맞춰 재노출 목록 조정(값 re-export 제거분 반영).
- `PostCard.tsx`/`PostList.tsx`: 타입만 소비하므로 변경 없음.

### 테스트

- `packages/shared-types`: `generate-problem-detail.mjs` 실행 후 산출물이 기존 수기 스키마와 필드 동등한지 스냅샷/수동 대조.
- `drift:check` 스크립트가 새 생성 파일도 포함하는지 확인(`git diff --exit-code` 대상 경로에 `src/generated` 전체가 이미 포함되어 있어 추가 설정 불필요 — 확인만).
- frontend: D 배치에서 신설되는 api-client 테스트가 `postIdSchema` 브랜드 검증·`problemDetailSchema` 파싱을 함께 커버.

## C. frontend — 캐시 무효화 정합

### 제약 조건 (조사로 확정)

- 설치된 Next.js `^16.2.10`(`apps/web/package.json`)이 `cacheComponents` 플래그로 `use cache`/`cacheTag`/`cacheLife`를 이미 지원한다 — 버전 업그레이드 불필요.
- `revalidateTag(tag, profile)`는 Next 16 기준 2-인자가 정식 시그니처(단일 인자는 deprecated).
- `cacheComponents: true`는 렌더링 시맨틱을 앱 전역으로 바꾸는 실험적 플래그다(Suspense 경계 요구 등) — board 페이지 외 다른 라우트에도 영향 가능. 이번 앱엔 board 라우트뿐이라 영향 범위는 제한적이나, 실사용 확인이 필요하다.

### 설계

- `apps/frontend/apps/web/next.config.ts`: `cacheComponents: true` 추가.
- `apps/frontend/apps/web/src/entities/post`: 캐시 태그 상수 신설(예: `postListTag()`, `postTag(id)`).
- `apps/frontend/apps/web/src/features/board/api/queries.ts`: 읽기 함수에 `'use cache'` + `cacheTag(...)` + `cacheLife(...)` 적용.
- `apps/frontend/apps/web/src/features/board/model/actions.ts`: 3개 Server Action(`createPostAction`/`updatePostAction`/`deletePostAction`)의 `revalidatePath` 호출을 `revalidateTag(tag, profile)` 또는 즉시 반영이 필요한 지점은 `updateTag`로 교체.

### 검증

- `pnpm dev`로 board 목록·상세·생성·수정·삭제 골든 패스를 브라우저에서 직접 확인해 캐시 무효화가 실제로 동작하는지 검증(자동 테스트만으로는 캐시 재검증 타이밍 확인이 어려움).

## D. frontend — 테스트 인프라 구축

### 설계

- 설치: `vitest`, `jsdom`, `@testing-library/react`, `msw`(devDependency, frontend 루트 또는 `apps/web`).
- 루트 `turbo.json`에 `test` 태스크 등록(dependsOn 등 기존 `lint`/`typecheck` 패턴 준용).
- 테스트 대상(기존 board 기능 한정):
  - `packages/api-client`: `getPosts`/`getPost`/`createPost`/`updatePost`/`deletePost` — MSW로 fetch 모킹, egress 검증 실패(불량 응답 드롭/throw) 케이스 포함.
  - `apps/web/src/entities/post/model.ts`: `formatDateTime`.
  - `apps/web/src/features/board/ui`: `PostForm`/`PostCard`/`PostList` — Testing Library.
  - `apps/web/src/features/board/model/actions.ts`: 성공 시 리다이렉트 호출, 검증 실패 시 필드 에러 반환 분기.
- 파일 배치: `{대상}.test.ts(x)` 대상 파일과 co-located. `__tests__` 디렉터리 생성 금지(`testing.md` 규칙).

### 범위 밖

- `page.tsx` 등 async Server Component 자체의 Vitest 단위 테스트 — RSC는 Vitest로 직접 렌더링 검증이 어렵다. `testing.md`가 Playwright를 opt-in으로 규정하므로 이번 배치엔 포함하지 않는다.
- MSW를 이용한 실제 백엔드 통합 시나리오(E2E) — 범위 밖.

## 배치 간 의존성

- A(backend)와 B/C/D(frontend·루트)는 언어가 달라 완전 독립.
- B는 C·D보다 먼저 끝나야 한다 — C/D가 `api-client`의 `postIdSchema`/`postResponseSchema` 등을 참조할 수 있으므로, D의 api-client 테스트 작성 시 B의 결과물(schemas.ts)이 이미 있어야 자연스럽다. B → D 순서 권장(단, 병렬 진행 자체는 가능하고 충돌 시 조정).
- C와 D는 서로 독립(캐시 로직과 테스트 인프라는 겹치지 않음). 단, D가 먼저 끝나면 C의 캐시 태그 교체를 테스트로 검증할 수 있어 D → C 순서가 약간 유리하나 필수는 아니다.

## 이번 배치에서 다루지 않는 것

- 나머지 위반 11건(spring-webflux 차단 목록 누락, actuator 노출/포트분리 미구성, 엔티티 인덱스 의도 미선언, dependency-cruiser 스캔 범위, `glossary.md` 부재, `cn` 유틸 부재, 타입드 에러 소유 패키지(`packages/entities`) 부재, Conform 미적용, 멱등키 미구현).
- 불확실 13건(강제 장치 부재로 표기된 ArchUnit 규칙 공백 등) — 별도 판단·배치 필요.

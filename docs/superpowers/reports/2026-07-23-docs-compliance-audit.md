# docs 가이드 준수 점검 보고서 (2026-07-23)

## 방법

- 대상 문서 15개: 루트 2(`docs/architecture.md`, `docs/sharing.md`) + backend 9(`apps/backend/AGENTS.md`, `apps/backend/docs/*.md` 7개, `DOMAIN_MODEL.md`, `REQUIREMENTS.md`) + frontend 7(`apps/frontend/AGENTS.md`, `apps/frontend/docs/*.md` 6개).
- 도메인별(루트/backend/frontend) 3개 독립 서브에이전트를 병렬 실행, 각 문서의 규칙 서술문을 실제 코드·설정과 정적 대조(파일 존재·내용·grep). 전체 빌드/테스트는 실행하지 않음.
- 판정: ✅ 구현됨 · ❌ 위반 · ⚠️ 불확실(검증 대상 코드 부재 또는 판단 근거 불충분).

## 요약

| 영역 | 위반 | 불확실 |
|---|---|---|
| 루트 (architecture.md·sharing.md) | 3 | 1 |
| backend | 4 | 3 |
| frontend | 8 | 9 |
| **합계** | **15** | **13** |

가장 심각한 것부터: frontend 테스트 인프라 전무, backend observability(RequestIdFilter) 미구현, 루트 shared-types 계약 생성물 원칙 위반. 상세는 아래 영역별 절 참고.

---

## 루트 (docs/architecture.md · docs/sharing.md)

| 문서 | 규칙 요약 | 상태 | 근거 |
|---|---|---|---|
| architecture.md:13-16 | 루트는 얇은 교차언어 허브, `docs/`는 architecture.md·sharing.md만 보유 | ✅ | `docs/`에 두 파일만 존재 |
| architecture.md:17-19 | backend/frontend가 각자 AGENTS.md·docs·서브트리 소유 | ✅ | 양쪽 확인 |
| architecture.md:20-21 | 프론트 standalone 설정 루트 흡수, 루트 설정 하나만 유효 | ✅ | `pnpm-workspace.yaml`·`turbo.json` 루트에 1개씩만 |
| architecture.md:26-42 | 루트 구조 다이어그램 | ✅ | 일치 |
| architecture.md:48-52 | 3레벨 공유 구조(`packages/*`, `apps/frontend/packages/*`, `apps/backend/module-common/*`) | ✅ | 전부 존재 |
| architecture.md:56 | 루트 `packages/shared-types`는 **계약 생성물만** 포함 | ❌ | `src/index.ts`가 손으로 작성됨(`postIdSchema` 브랜드 타입, `problemDetailSchema`, `.extend(...)` 조합). `codegen` 스크립트는 `src/generated/`만 갱신, `src/index.ts`는 수기 소스로 상존 |
| architecture.md:58-69 | `pnpm-workspace.yaml` 멤버 표 | ✅ | 표와 정확히 일치 |
| architecture.md:71 | `apps/backend/package.json`은 JS 워크스페이스 편입용, `./gradlew` 위임 | ✅ | `openapi`/`verify`/`clean` 전부 위임 |
| architecture.md:72 | 툴체인 버전 정본 = `.mise.toml` + 루트 `packageManager` | ✅ | 일치 |
| architecture.md:78-89 | 4분류 소유 표 | ✅ | root-infra 파일 전부 실재 |
| architecture.md:90 | 루트는 실행순서·게이트만, 계약필드·비즈니스규칙 미정의 | ✅ | 확인 |
| architecture.md:93 | AGENTS.md = 루트+2서브가이드 | ✅ | 확인 |
| architecture.md:103 | 의존 방향 강제 = dependency-cruiser(**설정은 루트 소유**) | ❌ | 설정 파일이 `apps/frontend/.dependency-cruiser.cjs` 하나뿐. 스캔 범위도 프론트 서브트리 내부(`apps/frontend/{apps,packages}`)로 국한돼 "TS 앱 → 루트 packages" 방향(폴리글랏 경계)은 실질적으로 검사되지 않음 |
| architecture.md:104 | Java 모듈 경계 = ArchUnit(backend 소관 확인) | ✅ | backend에서만 발견, 루트엔 미존재 — 소관 정합 |
| architecture.md:105 | 계약·생성물 일치 = drift 게이트 | ✅ | `drift:check` 스크립트로 codegen 재실행 후 git diff 검사 |
| architecture.md:106 | 계약 파괴적 변경 = oasdiff | ✅ | `scripts/oasdiff-check.sh` + CI 연결 |
| sharing.md:20-24 | 계약 원천 OpenAPI 코드퍼스트, `apps/backend/docs/openapi/`, springdoc·키정렬·스냅샷 테스트 | ✅ | `openapi.json` 1개, `OpenApiSnapshotTest`가 키 정렬 후 비교 |
| sharing.md:25-28 | 계약 방향 단방향(백엔드 originate) | ✅ | `orval.config.ts` input이 백엔드 산출물 |
| sharing.md:29-30 | tRPC·ts-rest 미사용 | ✅ | 문자열 미검출 |
| sharing.md:33 | 계약 표면에 토큰·내부 URL 미포함 | ✅ | 패턴 미검출 |
| sharing.md:74-85 | orval 코드젠, 계약당 엔트리·디렉터리 1개, `z.infer` 파생, 생성물 커밋 | ✅ | 전부 확인 |
| sharing.md:95-107 | 태스크 그래프·불투명 노드·소비 방향(`shared-types → api-client(server-only) → app`) | ✅ | `turbo.json`/`client.ts` 확인 |
| sharing.md:111-113 | 공용 에러 Zod는 계약 ProblemDetail 스키마에서 **생성**해 하나만 소유 | ❌ | orval이 에러 응답 스키마를 생성하지 않아 `problemDetailSchema`가 수기 작성(코드 주석이 자인). 단일 소유는 충족하나 "생성" 메커니즘 미충족 — 계약의 ProblemDetail이 바뀌어도 drift 게이트가 이 수기 스키마 불일치를 못 잡음 |
| sharing.md:115-121 | 기계 분기는 `code`만, `traceId` UI 미노출 | ✅ | 확인 |
| sharing.md:70 | "타입이 거짓말하는 지점" 처리 구현은 프론트 소비 계층 소유 | ⚠️ | 검증/분기 로직은 `api-client`(소비 계층)에 있으나 스키마 정의 자체는 루트 `shared-types`에 있어 "구현" 경계 판단 애매 |

**핵심 위반 3건**: `packages/shared-types` 수기 로직 혼입(architecture.md:56) · dependency-cruiser 설정 위치·스캔 범위 불일치(architecture.md:103) · 에러 Zod 스키마 "생성" 원칙 미충족(sharing.md:113).

---

## backend (apps/backend/AGENTS.md · docs/* · DOMAIN_MODEL.md · REQUIREMENTS.md)

실제 모듈 7개(`common-core`·`common-jpa`·`common-web`·`domain-board`·`app-api`·`app-migration`·`test-architecture`), board 단일 도메인. 문서가 "실물 필요 시 추가"라 명시한 모듈(`query-*`·`infra-*`·`common-auth` 등)은 부재 자체가 위반 아님 — 해당 규칙은 검증 대상 없음으로 표기.

| 문서 | 규칙 요약 | 상태 | 근거 |
|---|---|---|---|
| architecture.md | 베이스 패키지 `{그룹}.{모듈접두}.{모듈명}` | ✅ | 전 모듈 일치 |
| architecture.md | 컨벤션 플러그인이 계층별 의존 화이트리스트 강제 | ✅ | `ModuleBoundary.kt` |
| architecture.md | 도메인 모듈 3구역·의존 방향 | ✅ | `domain-board` 구조 일치 |
| architecture.md | 리포지토리 평탄 배치, `custom/` 미생성 | ✅ | `PostRepository` |
| architecture.md | 소프트삭제 엔티티 base finder 직접 호출 금지 | ✅ | ArchUnit `soft_delete_repositories_do_not_serve_base_finders` |
| architecture.md | provided 구현 package-private | ✅ | ArchUnit `provided_implementations_are_package_private` |
| architecture.md | JPA 매핑 타입 모듈 밖 비노출 | ✅ | ArchUnit 규칙 존재 |
| architecture.md | facade는 트랜잭션 미개방 | ✅ | ArchUnit 규칙 존재 |
| architecture.md | 앱은 required 직접 접근 금지 | ✅ | ArchUnit `apps_do_not_access_required_contracts` |
| architecture.md | 트랜잭션 경계(쓰기 `@Transactional`, 조회 `readOnly=true`) | ✅ | 확인 |
| architecture.md | 페이지네이션 1-based↔0-based 변환 소유 | ✅ | `PaginationRequest`/`PaginationResponse` + 테스트 |
| architecture.md | web 핸들러 문서화 애노테이션 강제, int `@RequestParam` 금지 | ✅ | ArchUnit 규칙 존재 |
| architecture.md | 아키텍처 테스트는 모듈 목록에서 동적 파생 | ✅ | `test-architecture/build.gradle.kts` |
| architecture.md | 서비스 역할 접미사·`Default{계약명}` 네이밍 | ✅ | ArchUnit `services_use_role_suffixes` |
| architecture.md | 컨트롤러 `web` 패키지·`Controller` 접미사 | ✅ | ArchUnit 규칙 존재 |
| architecture.md/code-quality.md | 금지 의존성(Lombok·H2·**spring-webflux**) 컨벤션 플러그인 차단 | ❌ | `convention.java-common.gradle.kts:6`의 `forbidden` 목록에 spring-webflux 누락. 현재 미사용 상태이나 강제 장치가 없어 향후 도입을 못 막음 |
| architecture.md | 전 모듈 `@NullMarked`를 아키텍처 테스트가 강제 | ⚠️ | 코드엔 실제로 존재(정상)하나 이를 검사하는 `@ArchTest`가 `ArchitectureTest.java`에 없음 — 강제 장치 자체 공백 |
| observability.md | logback/micrometer 임포트 제한을 아키텍처 테스트가 강제 | ⚠️ | 현재 위반 코드는 없으나 강제하는 `@ArchTest` 부재 |
| observability.md | `RequestIdFilter`가 상관 ID 소유(`X-Request-Id` 수용/생성·MDC `requestId`·응답 헤더·finally 제거) | ❌ | `RequestIdFilter` 클래스 자체가 존재하지 않음. `GlobalExceptionHandler`가 `MDC.get("traceId")`를 읽지만 이 키를 세팅하는 코드가 어디에도 없어 항상 null → 매 요청 랜덤 UUID로 대체됨 |
| observability.md | actuator: health/info/prometheus만 노출, 관리 포트 분리, probes 활성화 | ❌ | `application.yml`에 `health`만 노출(info/prometheus 없음, prometheus 레지스트리 의존성도 없음). 관리 포트 미분리, probes 설정 없음 |
| observability.md | 경계 예외 1회 로깅, 도메인 4xx 미로깅, SLF4J만 사용 | ✅ | `GlobalExceptionHandler` 확인, `System.out` 미검출 |
| entity-persistence.md | `BaseTimeEntity` 상속, Auditing | ✅ | ArchUnit `entities_extend_base_time_entity` |
| entity-persistence.md | `@Id`는 `create()`에서 `UuidV7Generator`, `@GeneratedValue` 미사용 | ✅ | 확인(단, 금지 자체를 강제하는 ArchUnit 규칙은 부재 — 현재 위반 없음이라 등급 낮음) |
| entity-persistence.md | 생성 진입점 단일화(`create()`, protected 기본 생성자) | ✅ | `Post.java` 확인 |
| entity-persistence.md | 소프트삭제(nullable `deletedAt`, 물리 DELETE 없음) | ✅ | 확인 |
| entity-persistence.md | `@Table(indexes=...)`로 인덱스 의도 선언, 마이그레이션의 파생 원천 | ❌ | `Post.java`의 `@Table`에 `indexes` 없음. 반면 `V1__create_post.sql`엔 부분 인덱스(`idx_post_active_latest`)가 존재 — 엔티티가 인덱스 의도를 선언하지 않은 채 마이그레이션만 앞서 나감 |
| coding-conventions.md | 타입 종류별 규칙(엔티티/Info/Request-Response/ErrorCode/Exception) | ✅ | 전수 확인 |
| coding-conventions.md | 클래스 접미사표 | ✅ | 전수 확인 |
| coding-conventions.md | `update`/`set`/`change` 범용 동사 금지 | ✅ | 확인 |
| code-quality.md | Spotless·NullAway·Error Prone 전 모듈 일괄 적용, 버전 표 | ✅ | `libs.versions.toml` 전부 일치 |
| code-quality.md | 의존성은 `libs.*` 카탈로그 별칭만 | ✅ | 버전 리터럴 미검출 |
| testing.md | 테스트 레벨표(단위/`@DataJpaTest`/`@WebMvcTest`/`@SpringBootTest`) | ✅ | 전 레벨 실물 확인 |
| testing.md | 영속 슬라이스 Testcontainers 싱글턴+Flyway 스키마 | ✅ | `ContainerConfig` 확인 |
| testing.md | 목·스텁 범위 제한(리포지토리·도메인 서비스 목 금지) | ✅ | 확인 |
| testing.md | 도구 고정, 금지 도구(Kotest 등) 미사용 | ✅ | 미검출 |
| caching.md | 캐시 도입은 합의 사항, 미도입 상태가 정상 | ✅(해당없음) | `@Cacheable` 등 미검출 — 원칙과 정합 |
| integration.md | 외부 HTTP는 `RestClient`만 | ✅(해당없음) | 외부 연동 자체 없음 |
| architecture.md | query 모듈 QueryDSL 기본/네이티브 최후수단 | N/A | `module-query` 미존재(도입 조건 미충족) |
| architecture.md | `app-migration`은 Flyway만, 실행 앱은 부팅 시 Flyway 미실행 | ⚠️ | `app-migration` 배치는 정상(`runtimeOnly`). 그러나 `app-api`가 `common-jpa`를 통해 flyway-core를 전이 의존으로 가지며 `spring.flyway.enabled=false` 등 비활성 설정이 없어, 부팅 시 자동 마이그레이션이 실행되지 않는다는 불변식이 코드로 보장되는지 정적으로는 불확실(런타임 미확인) |
| DOMAIN_MODEL.md/REQUIREMENTS.md | Post 필드·오퍼레이션·에러코드·페이지네이션 | ✅ | 정확히 일치 |
| REQUIREMENTS.md | OpenAPI 스냅샷이 계약 원천, drift 게이트 | ✅ | `OpenApiSnapshotTest` 확인 |

**핵심 위반 4건**: spring-webflux 차단 누락(convention.java-common.gradle.kts:6) · `RequestIdFilter` 미구현으로 상관 ID 체계 부재(observability.md) · actuator 노출/포트분리/probes 미구성(application.yml) · 엔티티 인덱스 의도 미선언(Post.java vs V1 마이그레이션).

---

## frontend (apps/frontend/AGENTS.md · docs/*)

| 문서 | 규칙 요약 | 상태 | 근거 |
|---|---|---|---|
| architecture.md | 워크스페이스 계층 단방향, packages↔apps 순환·교차 금지 | ✅ | `.dependency-cruiser.cjs` 규칙 4종 |
| architecture.md | `ui`/`config`/`api-client` 역할별 분리, 범용 util 패키지 금지 | ✅ | 확인 |
| architecture.md | `packages/entities`·`packages/auth` 워크스페이스 레벨 | ⚠️ | 앱 1개뿐이라 단일 앱 전용 예외로 정당화 가능(`entities`는 앱 로컬 존재) |
| architecture.md | FSD-lite 레이어·의존 방향 강제(boundaries 플러그인) | ✅ | `eslint-config/index.mjs` |
| architecture.md | features 간 직접 import 금지, widgets/pages 미사용 | ✅ | `no-unknown-files: error` |
| architecture.md | 세그먼트 `ui/model/api`, `index.client/server.ts` 분리 | ✅ | `features/board` 구조 확인 |
| architecture.md | `server-only`/`client-only` 포이즌 임포트 | ✅ | 확인 |
| architecture.md | `NEXT_PUBLIC_` 접두 분리 | ✅ | `env.client.ts` vs `env.ts` |
| architecture.md | `page.tsx` 얇게, 도메인 로직 미포함 | ✅ | 확인 |
| architecture.md | 기본 Node 런타임, Edge opt-in만, Pages Router 미사용 | ✅ | 확인 |
| architecture.md | env는 `packages/config`의 Zod로 부팅 시 검증 | ✅ | `safeParse` 확인 |
| architecture.md | 프론트 standalone 설정 루트 흡수 | ✅ | 확인 |
| architecture.md | dependency-cruiser·eslint-boundaries 강제, steiger 미사용 | ✅ | 확인 |
| code-quality.md | 품질 게이트 중앙 상속(ESLint/tsconfig/dependency-cruiser/turbo) | ✅ | 확인 |
| code-quality.md | Prettier + eslint-config-prettier | ✅ | 확인 |
| code-quality.md | 필수 플러그인(next/react-hooks/jsx-a11y/boundaries), 원시값 차단 | ✅ | `no-raw-tailwind-values.mjs` |
| code-quality.md | Biome·Ladle 미사용 | ✅ | 확인 |
| code-quality.md | `tsc --noEmit`(project references) | ⚠️ | `strict: true`는 충족하나 TS project references(`composite`/`references`) 미사용, turbo 그래프로 대체 |
| code-quality.md | Storybook은 디자인시스템 소유, a11y 게이트 | ✅ | `test-runner.ts` 라이트/다크 axe |
| code-quality.md | `eslint-disable`/`@ts-ignore` 최소화 | ✅ | 0건 |
| code-quality.md | 버전 baseline | ✅ | 확인 |
| coding-conventions.md | 컴포넌트/라우트 export 규칙 | ✅ | 확인 |
| coding-conventions.md | 벤더 중립 명명 | ✅ | 확인 |
| coding-conventions.md | 도메인 표준어는 슬라이스 루트 `glossary.md` 소유 | ❌ | `apps/frontend` 전체에 `glossary.md` 부재 |
| coding-conventions.md | 타입은 `z.infer`만, `any` 미사용 | ✅ | 확인 |
| coding-conventions.md | 서버/클라 경계 가로지르는 배럴 금지 | ✅ | 확인 |
| coding-conventions.md | 재사용 컴포넌트 `packages/ui`, 도메인 로직 미포함 | ✅ | 확인 |
| coding-conventions.md | 접근성(시맨틱·label 연결) | ✅ | 확인 |
| coding-conventions.md | 조건부 클래스는 `packages/ui` 소유 `cn` 유틸로 합성 | ❌ | `packages/ui`에 `cn` export 없음. `Button`/`Alert`/`TextField`/`TextAreaField` 전부 `clsx` 직접 사용 |
| coding-conventions.md | 주석 한국어, TSDoc 형식 | ✅ | 확인 |
| design-system.md | 토큰 2계층, 컴포넌트 직접 색·치수 선언 금지 | ✅ | `tokens.css` + ESLint 규칙 |
| design-system.md | 컴포넌트 계층 분리(프리미티브/조합/도메인UI) | ✅ | 확인 |
| design-system.md | variant/size 유한 prop, className 배치 전용 | ✅ | 확인 |
| design-system.md | 완료 정의(상태·라이트다크·모션·reduced-motion) | ✅ | `motion-safe:` 확인 |
| design-system.md | 워크벤치 스토리 = 사용 계약 | ✅ | 전 컴포넌트 stories 존재 |
| rendering.md | 기본 RSC, `'use client'` leaf 최소화 | ✅ | 확인 |
| rendering.md | Hydration 결정성(서버 렌더에 Date.now/Math.random/window 금지) | ✅ | UTC 고정 포맷 확인 |
| rendering.md | `params`/`searchParams` await | ✅ | 확인 |
| rendering.md | `redirect()`/`notFound()` try/catch로 삼키지 않음 | ✅ | 확인 |
| rendering.md | async 상태 파일(loading/error/not-found/global-error) | ⚠️ | `global-error.tsx`가 어디에도 없음(나머지는 존재) |
| rendering.md | 메타데이터 규칙(metadataBase/await/viewport) | ✅ | 확인 |
| rendering.md | fetch 요청 단위 메모이제이션 | ✅ | `cache(fetchPost)` |
| data.md | read-model은 Zod+순수 파생만 | ✅ | 확인 |
| data.md | 읽기 RSC/route handler, 변경 Server Action | ✅ | 확인 |
| data.md | 클라이언트 상태는 URL 우선 | ✅ | 확인 |
| data.md | 목록 항목별 개별 페치 금지 | ✅ | 단일 호출 확인 |
| data.md | 백엔드 호출은 `api-client`만 | ✅ | 확인 |
| data.md | egress 검증(봉투 throw, 항목 개별 drop) | ✅ | `readValidatedPage` |
| data.md | 상세 egress 실패는 타입드 에러 throw | ✅ | 확인 |
| data.md | 타입드 에러 타입은 `packages/entities`가 소유 | ❌ | `ApiError`가 `packages/api-client/src/error.ts`에 위치. `packages/entities` 패키지 자체가 없음 |
| data.md | 캐시 태그 taxonomy(entities 소유), `use cache`+`cacheTag`/`cacheLife`, `revalidateTag(tag, profile)`, `updateTag` | ❌ | 관련 API 전부 0건. 대신 문서에 없는 `revalidatePath`만 사용 |
| data.md | ingress 엄격 검증, 표시 파라미터만 `.catch` 보정 | ✅ | 확인 |
| data.md | 성공 응답 envelope 없음, offset pagination meta | ✅ | 확인 |
| data.md | Server Action 변경 전용, 인가 판정 | ⚠️ | 변경 전용은 충족, 인증 기능 자체 미도입이라 인가 로직 검증 불가 |
| data.md | 폼: `useActionState`+Conform, 동일 Zod 스키마 클라·서버 재사용 | ❌ | Conform 의존성 자체가 없음. `useActionState`만 단독 사용, 클라이언트 측 스키마 재사용 없음 |
| data.md | 변경 요청에 클라이언트 생성 멱등키 포함 | ❌ | create/update/delete 전 구간에 멱등키 코드 없음 |
| testing.md | 테스트 도구 baseline(Vitest/jsdom/Testing Library/MSW) 설치 | ❌ | 관련 의존성 0건, 설정 파일 없음 |
| testing.md | 테스트 파일 배치·실제 테스트 존재 | ❌ | `*.test.ts(x)`/`*.spec.ts(x)` 파일 0개 |
| testing.md | 테스트 태스크 turbo 그래프 등록 | ❌ | `turbo.json`에 `test` 태스크 자체 없음 |
| testing.md | 포이즌 임포트를 테스트 셋업에서 무해화 | ⚠️ | 테스트 러너 자체가 없어 검증 대상 없음(위 항목에 종속) |
| sharing.md(위임) | UUIDv7/Instant/int64/ProblemDetail code 처리 | ✅ | 브랜드 타입·ISO 문자열·string 유지·open string 전부 확인 |
| sharing.md(위임) | BigDecimal/enum/nullable/oneOf 처리 | ⚠️×4 | 현재 계약(Post)에 해당 필드가 없어 검증 대상 자체 없음 |
| sharing.md | 소비 방향 `shared-types → api-client(server-only) → app` | ✅ | 확인 |

**핵심 위반 8건**: `glossary.md` 부재 · `cn` 유틸 부재(clsx 직접 사용) · 타입드 에러가 `packages/entities` 아닌 `api-client`에 위치(패키지 자체 부재) · 캐시 태그 taxonomy·`cacheTag`/`revalidateTag` 미구현(`revalidatePath`로 대체) · 폼 Conform 미적용 · 멱등키 미구현 · **테스트 인프라·테스트 파일·turbo test 태스크 전무**(가장 심각).

---

## 종합 우선순위 제안 (참고용, 실행 여부는 별도 판단)

1. **frontend 테스트 인프라 0** — 문서가 명시한 Vitest/Testing Library/MSW 자체가 없고 테스트 파일이 한 개도 없음. 회귀 안전망 부재.
2. **backend `RequestIdFilter` 미구현** — 관측성 근간(상관 ID)이 빠져 있어 `traceId` 로깅이 사실상 무의미(항상 랜덤 재생성).
3. **루트 `packages/shared-types` 수기 로직 혼입** — drift 게이트가 이 부분을 검사 못 해 계약 변경 시 조용히 깨질 수 있음.
4. **frontend 캐시 무효화가 문서 규정과 다른 메커니즘(`revalidatePath`)으로 구현** — 설계 문서와 실제 캐시 전략이 괴리.
5. 나머지(actuator 설정, dependency-cruiser 범위, glossary.md, cn 유틸, 멱등키, Conform, 인덱스 의도 선언 등)는 개별 판단 필요.

이 보고서는 점검 전용이며 코드 수정은 포함하지 않음.

# WORKPLAN — 코드를 가이드에 정렬 (검수 후속 작업 계획)

- 정본(수정 금지): 루트 `docs/architecture.md`·`docs/sharing.md`, `apps/backend/docs/*`, `apps/frontend/docs/*`, 각 `AGENTS.md`
- 원칙: **코드가 가이드를 따른다.** 이 계획의 어떤 작업도 가이드 문서를 수정하지 않는다. 문서 간 상충으로 진행이 불가능하면 해당 작업을 "보류" 처리하고 다음 작업으로 넘어간다.
- 배경: 전체 구조 검수 결과 루트 topology·seam 설계·프론트 실코드는 대체로 정합, 백엔드 구조와 "빌드가 강제한다"고 선언된 게이트들에 가이드 대비 격차가 확인됨. 근거는 각 작업의 `근거` 필드 참조.

## 확정 해석 (루프가 재논의하지 않는다)

1. 앱의 `@polyglot-board/shared-types` 직접 import(타입·Zod 스키마 소비)는 위반이 아니다. `docs/sharing.md`가 소비 정책을 프론트 소유로 위임하고, `apps/frontend/docs/data.md`는 "백엔드 호출"만 api-client 경유를 요구한다.
2. 문서가 미래 상태를 선기술한 부분(`app-migration`, `module-tests`, `PaginationRequest` 등)은 "코드를 가이드에 맞춘다" 기준에 따라 **구현 대상**이다.
3. 커밋은 작업(Txx) 단위 1커밋. 브랜치는 `feat/guide-alignment`를 사용한다(없으면 main에서 생성).

## 실행 프로토콜 (루프 반복 1회 = 작업 1개)

0. 전제: 리포 루트에서 실행. `feat/guide-alignment` 브랜치 확인/생성. 툴체인은 `.mise.toml`(java corretto-25, node 24, pnpm 9). 백엔드 테스트는 Docker(Testcontainers) 필요 — 미가용이면 해당 게이트를 "환경 미가용"으로 로그에 남기고 나머지 게이트로 진행.
1. **작업 선택**: 아래 "작업 목록"에서 미완료 `[ ]` 중 최상단 1개. `보류:` 표시 작업은 건너뛴다.
2. **표면 분류·문서 로딩**: 작업의 `표면` 필드에 따라 루트 `AGENTS.md` 트리아지대로 소유 가이드를 로딩한다.
   - backend-only → `apps/backend/AGENTS.md` + 관련 `apps/backend/docs/*`
   - frontend-only → `apps/frontend/AGENTS.md` + 관련 `apps/frontend/docs/*`
   - cross-language·root-infra → 루트 `docs/architecture.md`·`docs/sharing.md` (+필요 시 양쪽 서브가이드)
3. **설계**: 가정·해석·트레이드오프를 명시하고(서브가이드 작업 원칙 1), 작업의 `완료 기준`을 검증 가능한 단계 계획(`단계 → 검증`)으로 구체화한다(작업 원칙 4). 과설계 여부를 자문한다(작업 원칙 2).
4. **설계 리뷰**: 독립 서브에이전트 1개를 띄워 설계를 반박시킨다(관점: 가이드 정합성 / 과설계 / 계약·게이트 파급). 타당한 지적을 반영한다.
5. **구현**: 외과적으로, 최소 코드로(작업 원칙 2·3). `docs/*.md`·`AGENTS.md`·`CLAUDE.md`는 수정 금지.
6. **자기검증·코드 리뷰**: 루트 `AGENTS.md` 규정대로 `docs/architecture.md` 경계 강제와 `docs/sharing.md` 타입 무결성으로 자기검증한다. 이어 독립 서브에이전트로 diff를 리뷰시키고(가이드 위반·버그·회귀) 지적을 반영한다.
7. **게이트 실행** (깨진 채로 반복을 끝내지 않는다):
   - frontend-only: `pnpm verify` (turbo verify + depcruise + drift:check 포함)
   - backend-only: `cd apps/backend && ./gradlew build` + 계약 표면이 변했으면 루트에서 `pnpm openapi && pnpm codegen && pnpm run drift:check`
   - cross-language·root-infra: `pnpm verify` 전체
8. **마감**: WORKPLAN.md의 체크박스를 `[x]`로 갱신하고 "작업 로그"에 1–3줄(커밋 해시·핵심 결정) 추가. 변경 전체를 1커밋(`type(scope): 요약`, 본문에 작업 ID).
9. **예외**: 문서 상충·인간 결정 필요·환경 제약으로 진행 불가 → 코드 변경 원복, 해당 작업 앞에 `보류:` + 사유를 기록하고 반복을 종료한다(다음 반복이 다음 작업 진행).
10. **반복 종료**: 이번 반복의 작업 1개가 마감되면 그냥 턴을 끝낸다. 랄프루프 stop hook이 같은 프롬프트를 다시 공급하며, 다음 반복은 WORKPLAN.md 체크박스·작업 로그·git log에서 이전 진행 상황을 확인하고 이어간다. 한 반복에서 두 개 이상의 작업을 시작하지 않는다.
11. **루프 완주 조건**: 모든 작업이 `[x]`이거나 남은 것이 전부 `보류:`면 → `pnpm verify` 최종 실행, "작업 로그"에 총괄 요약 기록 후 완료 약속 문구 `WORKPLAN COMPLETE`를 정확히 출력한다. **이 조건이 완전히 참이 아닌 한 어떤 상황에서도 이 문구를 출력하지 않는다**(막혔다고 탈출용으로 쓰지 않는다 — 막히면 9번 보류 절차).

---

## 작업 목록

### P0 — 사고 복구

- [x] **T1. OpenAPI 계약 파일 복구·재커밋** — 표면: cross-language(루트 소유)
  - 근거: `docs/sharing.md` "계약 파일은 `apps/backend/docs/openapi/`에 둔다", "생성물은 커밋한다". 커밋 `eb72d33`이 `apps/backend/docs/openapi/openapi.json`(453줄)을 docs 동기화에 섞어 삭제 — 현재 orval input dangling, 스냅샷 테스트 즉시 실패 상태.
  - 내용: `pnpm openapi`로 재생성 → `pnpm codegen` 후 `packages/shared-types/src/generated` 무변경 확인 → openapi.json 커밋.
  - 완료 기준: openapi.json이 git 추적됨. `pnpm run drift:check` 통과. `./gradlew :module-apps:app-api:test`의 OpenApiSnapshotTest 통과.

### P1 — 게이트 복구·신설 (root-infra)

- [x] **T2. drift:check의 삭제·untracked 허점 보강** — 표면: root-infra
  - 근거: `docs/sharing.md` "drift 게이트는 계약과 생성물의 일치를 강제한다". 현행 `git diff --exit-code`는 untracked 파일을 비교하지 않아 계약 파일이 HEAD에서 삭제된 상태(최대 drift)를 통과시킴 — T1의 사고가 실제로 통과한 경로.
  - 내용: 루트 `package.json`의 `drift:check`가 대상 경로의 untracked·삭제 상태도 실패시키도록 보강(방식은 설계에서 결정, 예: `git status --porcelain` 병행 검사 또는 `git add -N` 후 diff).
  - 완료 기준: openapi.json을 임시 삭제한 상태에서 `pnpm run drift:check`가 non-zero exit(검증 후 원복). 정상 상태에서 통과.

- [x] **T3. CI 워크플로 도입** — 표면: root-infra
  - 근거: `docs/architecture.md` "경계는 리뷰가 아닌 검사로 강제한다", `docs/sharing.md` "…CI가 실패한다". 현재 `.github/` 부재로 모든 게이트가 로컬 수동 실행 의존.
  - 내용: `.github/workflows`에 verify 파이프라인 신설 — mise 기반 툴체인(corretto-25·node 24·pnpm 9), Docker 가용 러너에서 `pnpm verify` 실행. `apps/frontend/docs/code-quality.md`의 "로컬 명령과 CI 명령은 동일하다"를 준수(스크립트 재사용, CI 전용 로직 금지).
  - 완료 기준: 워크플로 파일 존재, 실행 명령이 루트 `package.json` 스크립트와 동일, 로컬에서 동일 명령 통과.

- [x] **T4. oasdiff 파괴적 변경 게이트 배선** — 표면: root-infra
  - 근거: `docs/architecture.md` 강제 장치 표 "계약 파괴적 변경 | oasdiff", `docs/sharing.md` "파괴적 변경(삭제·의미 변경)은 oasdiff로 검사한다". 현재 저장소 어디에도 배선 없음.
  - 내용: CI에서 base 브랜치의 openapi.json 대비 breaking 검사 단계 추가(도구 설치·실행 방식은 설계에서 결정). 로컬 실행 스크립트도 제공하면 T3의 로컬=CI 동일성 유지.
  - 완료 기준: 워크플로에 oasdiff 단계 존재. 파괴적 변경을 임시로 만들어 로컬 시뮬레이션 시 검출됨(검증 후 원복).

### P2 — 백엔드 구조 정렬 (backend-only, 계약 표면 변경 시 cross-language 승격)

- [ ] **T5. `presentation` → `web` 패키지 정렬 + 리소스 슬라이스** — 표면: backend-only
  - 근거: `apps/backend/docs/architecture.md` "중심 패키지: `web`, `facade`", "`web/v{n}`은 버전을 바깥 축, 리소스를 안쪽 축으로 구성해 `/api/v{n}/{리소스}` URL을 미러링한다". 실제는 `com.board.api.presentation.v1` 평탄 구조이며 ArchUnit이 반대 배치를 강제 중.
  - 내용: `com.board.api.presentation.v1` → `com.board.api.web.v1.post`로 이동(컨트롤러+DTO), ArchUnit `controllers_reside_in_presentation` 등 관련 규칙 동기 수정, 통합 테스트 패키지 미러링(`web.v1.post`)까지 함께. facade 응답 소유(architecture.md의 "facade가 응답을 만든다"·"`*View`는 facade/view 소유") 해석을 설계 단계에서 확정하고 필요 시 반영.
  - 주의: 패키지 이동이 계약 표면(경로·operationId·스키마명)을 바꾸지 않는지 확인 — 바뀌면 cross-language 승격(T1 파이프라인으로 재방출·재생성 커밋).
  - 완료 기준: `presentation` 패키지 0건. `./gradlew build` 통과. `pnpm run drift:check` 통과.

- [ ] **T6. `module-tests/test-architecture` 신설·아키텍처 테스트 이전** — 표면: backend-only
  - 근거: `apps/backend/docs/architecture.md` "아키텍처 테스트는 `module-tests/test-architecture` 모듈이 소유한다", "앱이나 애플리케이션 모듈 안에 두지 않는다", "검증 대상은 모듈 목록에서 파생한다". 현재 app-api 테스트 소스에 위치, 대상 패키지 하드코딩.
  - 내용: `:module-tests:test-architecture` 모듈 신설(settings 등록, 필요 시 계층 컨벤션 플러그인 — 문서의 "첫 모듈 생성 시 함께 구현" 규칙 준수), ArchitectureTest 이전, 검증 대상 패키지를 모듈 목록에서 파생하도록 개선, base finder 금지 규칙에 문서 예외(`deletedAt` 없는 엔티티 허용) 반영·board 도메인 하드코딩 제거.
  - 완료 기준: app-api 테스트 소스에 ArchUnit 부재. `./gradlew build`에서 신규 모듈 테스트가 실행·통과.

- [ ] **T7. OpenAPI 애노테이션 부착 + ArchUnit 강제 규칙 2종 추가** — 표면: backend-only → 계약 표면 변경으로 cross-language 승격
  - 근거: `apps/backend/docs/architecture.md` "`web` 핸들러와 request/response는 `@Operation`, `@ApiResponse`, `@Parameter(description)`, `@Schema`로 명시한다"(아키텍처 테스트 강제 대상), "int·Integer `@RequestParam` 직접 선언 금지"(동). 현재 애노테이션 0건, 강제 규칙 자체가 미구현.
  - 내용: BoardController와 request/response 전체에 애노테이션 부착. test-architecture에 (a) web 핸들러·DTO 애노테이션 필수 (b) int·Integer `@RequestParam` 직접 선언 금지(페이징 파라미터 예외) 규칙 추가.
  - 완료 기준: 신규 ArchUnit 규칙 2종 통과. 계약 재방출·codegen 후 `pnpm verify` 통과, 생성물 커밋.
  - 의존: T6 이후.

- [ ] **T8. 페이지네이션 공통화 + 1-based 계약 전환** — 표면: cross-language(루트 소유, 계약 필드는 백엔드 소유)
  - 근거: `apps/backend/docs/architecture.md` "페이징 GET은 common-web `PaginationRequest`(`@Valid @ParameterObject`)를 사용한다", "클라이언트 요청은 1-based", "응답은 common-web `PaginationResponse`의 `page` 컴포넌트, 도메인 0-based를 1-based로 보정". 현재 common-web에 타입 부재, 컨트롤러가 `int page`(0-based) 직접 선언, 앱 로컬 PageResponse가 무보정.
  - 내용: common-web에 `PaginationRequest`·`PaginationResponse` 신설, BoardController 적용(앱 로컬 PageResponse 대체), 0↔1 보정, 계약 재방출 → codegen → 프론트 소비(queries·PostList 등) 1-based 반영.
  - 주의: 의미 변경 = 파괴적 변경. `docs/sharing.md`의 expand-and-contract 필요성을 설계에서 판단(모노레포 단일 소비자면 원자 반영 근거를 설계에 명시). oasdiff(T4)가 검출하는 것이 정상 — 의도된 breaking으로 처리하는 절차 포함.
  - 완료 기준: 백엔드 통합 테스트가 1-based 요청·응답 검증. `pnpm verify` 전체 통과.
  - 의존: T5, T7 이후 권장.

- [ ] **T9. `app-migration` 신설 + Flyway 실행 이전** — 표면: backend-only
  - 근거: `apps/backend/docs/architecture.md` "최소 구성은 `app-api`와 `app-migration`", "`app-migration`이 마이그레이션을 실행한다. 실행 앱은 부팅 시 Flyway를 실행하지 않는다", "각 도메인의 Flyway 인스턴스는 `SchemaFlywayFactory`(`common-jpa`)에 등록하고 `app-migration`이 실행한다". 현재 app-api가 부팅 시 migrate 실행.
  - 내용: `:module-apps:app-migration` 신설(실행→migrate→종료), `SchemaFlyway`를 문서 명칭·역할(`SchemaFlywayFactory`)로 정렬, app-api에서 FlywayConfig·flyway 의존 제거(부팅 시 미실행), 통합 테스트(Testcontainers)의 스키마 준비 흐름 갱신, `ddl-auto: validate` 유지.
  - 완료 기준: app-api 부팅 시 Flyway 미실행(의존·로그 확인). app-migration 실행으로 스키마 생성. `./gradlew build` 통과.

- [ ] **T10. build-logic 경계 장치 정비** — 표면: backend-only
  - 근거: `apps/backend/docs/architecture.md` "경계는 컴파일 의존성으로 강제한다"(compileOnly 미검사 우회 존재), domain-module 의존 가능 대상에 `domain-shared` 포함(화이트리스트 누락), "모듈 간 의존은 기본 `implementation`, `api`는 공개 시그니처 재노출 시만"(위반 3건).
  - 내용: `convention.domain-module` 화이트리스트에 `:module-domains:domain-shared` 추가(주석 동기화), `ModuleBoundary.kt`가 compileOnly 프로젝트 의존도 검사하도록 확장, api 최소화 — common-jpa의 미사용 `api(common-core)` 정리, common-core `api(uuid-creator)` → implementation, common-jpa `api(spring-boot-flyway)`는 T9 결과를 반영해 정리.
  - 완료 기준: `./gradlew build` 통과. compileOnly 우회 케이스를 임시 추가 시 빌드 실패(검증 후 원복).
  - 의존: T9 이후 권장(flyway 의존 정리 중복 방지).

- [ ] **T11. 모듈 경계 표식·테스트 미러링 잔여 정리** — 표면: backend-only
  - 근거: `apps/backend/docs/architecture.md` "각 모듈의 루트에는 모듈 경계와 계약을 드러내는 표식을 둔다(모듈 단위 문서화)", "테스트 코드는 대상의 패키지 구조를 그대로 반영". 현재 루트급 package-info 부재/무주석, 테스트 미러링은 T5에서 처리되지 않은 잔여분 확인.
  - 내용: 각 모듈 루트 패키지 package-info.java 신설·경계/계약 Javadoc 서술(전 모듈), 테스트 패키지 미러링 잔여 확인·수정.
  - 완료 기준: 모든 모듈 루트 패키지에 서술 있는 package-info 존재. `./gradlew build` 통과.

### P3 — 프론트엔드 강제 격차 해소 (frontend-only)

- [ ] **T12. 중앙 게이트 설정 패키지 신설 (`packages/eslint-config`·`packages/tsconfig`)** — 표면: frontend-only
  - 근거: `apps/frontend/docs/code-quality.md` "강제 기계는 중앙 설정이 소유한다. ESLint: `packages/eslint-config`, TypeScript: `packages/tsconfig`", "강제 로직을 앱마다 복제하지 않는다". 현재 boundaries 로직이 apps/web 로컬 인라인, ts 공유는 파일(tsconfig.base.json) 형태.
  - 내용: `apps/frontend/packages/eslint-config`·`packages/tsconfig` 신설, apps/web과 각 패키지가 상속하도록 전환, 루트 pnpm-workspace glob(`apps/frontend/packages/*`)에 자동 포함되는지 확인.
  - 완료 기준: `pnpm lint`·`pnpm typecheck` 통과. apps/web eslint 설정에 규칙 본문 없음(상속만). depcruise 통과.

- [ ] **T13. boundaries 강제 정비 (deep import·no-unknown·v7 문법·레이어 표 정렬)** — 표면: frontend-only
  - 근거: `apps/frontend/docs/architecture.md` "public API 우회 deep import 금지"(현재 미강제 — 실증됨), "`widgets`, FSD `pages` 사용 금지"(no-unknown off로 미강제), 레이어 표의 app 의존 대상 "features"(현재 코드는 app→entities import, eslint는 entity·shared까지 허용), boundaries v7에 v5 레거시 문법(셀렉터 경고 2건).
  - 내용: (a) FSD 슬라이스 public API(index.server/client) 우회 import를 error로 차단(boundaries entry-point 또는 depcruise 규칙 — 설계에서 결정), (b) `boundaries/no-unknown`·`no-unknown-files` 활성화, (c) v7 문법 마이그레이션으로 경고 제거, (d) app 레이어 허용을 문서 표에 정렬 — app의 `@/entities/post` 직접 import는 feature public API 재노출 등으로 대체. 문서 내 상충 발견 시 보류 절차.
  - 완료 기준: deep import·`src/widgets` 생성의 임시 위반 재현 시 lint 실패(검증 후 원복). 현재(수정 후) 코드는 lint·verify 통과.
  - 의존: T12 이후(중앙 설정에서 구현).

- [ ] **T14. 포이즌 임포트·고아 규칙 보강** — 표면: frontend-only
  - 근거: `apps/frontend/docs/architecture.md` "`server-only`, `client-only` 포이즌 임포트로 서버·클라 누수를 빌드 에러로 차단"(client-only 미구현), "서버 전용 코드는 `api`에 두고 `server-only`로 가드"(queries.ts 자체 가드 없음, 전이 의존), depcruise `no-orphans`가 warn이라 게이트 실패 불가.
  - 내용: `features/board/api/queries.ts`에 `import 'server-only'` 직접 가드, `no-orphans` severity error 승격(스토리북 등 정당 예외는 exclude로 처리), client-only는 적용 대상 모듈이 현존하는지 설계에서 판단 — 대상이 없으면 과설계 금지 원칙에 따라 도입 보류를 로그에 남김.
  - 완료 기준: `pnpm verify` 통과. queries.ts 가드 존재. 고아 모듈 임시 생성 시 depcruise 실패(검증 후 원복).

- [ ] **T15. 타입 정직성 보강 (UUID 브랜드·int64 매핑)** — 표면: cross-language(소비 측 중심)
  - 근거: `docs/sharing.md` 타입 정직성 표 — "UUIDv7 | plain string + 브랜드 타입·format 검증"(브랜드 미구현), "int64·`long` | string 또는 bigint"(totalElements가 number, `packages/shared-types/src/index.ts`에 규모 근거 주석으로 일탈 중).
  - 내용: shared-types 경계에서 UUID 브랜드 타입 도입(방식은 설계에서 결정 — 예: zod `.brand()`), int64 필드의 string/bigint 정렬(계약 표현 변경 vs 소비 측 변환 중 설계에서 결정), 기존 일탈 주석·구현 대체, 프론트 소비 코드 갱신.
  - 완료 기준: sharing.md 표와 실제 매핑 일치. `pnpm verify` 통과.
  - 의존: T8 이후(페이지네이션 응답 형상 확정 후).

---

## 보류 / 결정 필요

(루프가 진행 불가 작업을 여기에 사유와 함께 기록한다)

## 작업 로그

(반복마다 1–3줄: 작업 ID, 커밋 해시, 핵심 결정)

- T1: `pnpm openapi` 코드퍼스트 재방출로 복구(가이드 정본 경로). 삭제 직전(`eb72d33^`)과 바이트 동일 검증, codegen 무변경, OpenApiSnapshotTest `--rerun` 통과, `pnpm verify` 전체 통과. 커밋 `bc4e666`.
- T2: drift:check에 `git ls-files --others --exclude-standard` fail-closed 체인 추가 — untracked·HEAD 삭제 사고 상태 검출(T1 사고 경로 재현으로 실증). staged 포함 porcelain 전체 검사는 정상 stage→verify 플로우를 깨는 DX 회귀라 기각(설계 리뷰 합의). 잔여 구멍(로그·후속 후보): staged-stray, 커밋된 stray(근본 해결은 codegen clean-regeneration), diff·ls-files 절의 경로 목록 2회 중복. 커밋 `c7f4cff`.
- T3: `.github/workflows/verify.yml` 신설 — mise-action(SHA 핀, mise 2026.7.11 핀, `env: true`로 JAVA_HOME→corretto-25)·setup-gradle(SHA 핀) 후 `pnpm install --frozen-lockfile && pnpm verify`만 실행(CI 전용 로직 0). push 전 브랜치+PR 트리거(직push 워크플로우 대응), main은 cancel 제외. 검증: actionlint 1.7.12 통과, 로컬 CI 동일 시퀀스 통과. 한계·후속: 원격 첫 실행은 push 전이라 미검증 / required status check·브랜치 보호는 리포 설정 영역 / T4는 base ref fetch 추가 필요(fetch-depth 1 부족) / .mise.toml fuzzy 버전(`"9"` 등) 정밀 핀은 별도 root-infra 판단.
- T4: `scripts/oasdiff-check.sh` + 루트 `oasdiff:check` 스크립트 + CI 스텝(fetch-depth 0) 신설. base=merge-base(HEAD, origin/main), base에 계약 없으면 신규 계약 skip-pass(현 origin/main 상태), Docker `tufin/oasdiff:v1.24.0` digest 핀, stdin+단일 파일 ro 마운트(Colima 호환), `--fail-on ERR`(WARN 승격·의도적 breaking용 err-ignore는 T8에서 판단). verify 체인 미포함(비밀폐적 게이트라 별도 스텝 — 설계 리뷰 합의). 검증: DELETE 오퍼레이션 임시 제거 시 exit 1 검출 후 원복, 무변경 exit 0, actionlint·`pnpm verify` 통과. 한계: main push는 self-compare 공허 통과(사전 게이트는 브랜치 push/PR에서 발화).

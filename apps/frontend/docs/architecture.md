# Architecture

## 언제

- 새 앱·공유 패키지·feature 슬라이스를 추가하거나 모듈 경계를 정할 때.
- 어떤 계층이 어떤 계층을 의존해도 되는지, 새 코드를 어느 워크스페이스·레이어·세그먼트에 둘지 정할 때.
- 서버/클라 경계가 구조적으로 어디에 떨어지는지, 시크릿이 어느 쪽에 사는지 정할 때. 무엇을 서버·무엇을 클라에서 실행할지의 판단은 → [rendering](rendering.md)의 렌더 경계.
- 공유 도메인 모델(read-model)을 어디에 둘지 정할 때. 그 모델이 담는 것·검증은 → [data](data.md)의 read-model.

## 규칙

### 2계층 모듈 지도

- 모듈은 두 계층의 경계와 의존 방향을 지킨다. 의존은 항상 한 방향으로만 흐른다. 경계는 리뷰가 아니라 빌드가 강제한다(아래 빌드가 강제하는 불변식).
- 계층 1 — 워크스페이스(Turborepo + pnpm workspaces).

  | 워크스페이스          | 역할                                                     | 의존 가능 대상  |
  | --------------------- | -------------------------------------------------------- | --------------- |
  | `apps/*`              | 실행 Next.js 앱 — 라우팅·조립·표현(web · admin …)        | packages 전부   |
  | `packages/entities`   | 공유 도메인 read-model·타입·Zod 스키마                   | packages(하위)  |
  | `packages/ui`         | 디자인시스템 컴포넌트                                    | packages(하위)  |
  | `packages/api-client` | 백엔드 타입드 클라이언트(server-only)                    | packages(하위)  |
  | `packages/auth`       | 세션·토큰 원자재(server-only core + 클라 훅 분리)        | packages(하위)  |
  | `packages/config`     | 공용 설정·상수·env 스키마                                | 제로 도메인     |

- 구현된 워크스페이스는 `apps/web`·`packages/ui`·`packages/api-client`·`packages/config`다. `packages/entities`·`packages/auth`는 미구현(향후)이다 — read-model은 현재 앱 로컬 `entities/`에 있다(아래 read-model·api-client 배치).
- 단방향 의존: `apps/*`가 `packages/*`를 의존한다. 역방향(packages→apps)·앱 간 의존(appA→appB)·패키지 순환은 금지한다.
- 공유는 사용처로 가른다: 둘 이상의 앱이 쓰면 `packages/*`로 승격하고, 한 앱 전용이면 그 앱 안 `src/`에 둔다. 애매하면 앱 로컬로 두고 두 번째 앱이 필요로 할 때 승격한다.
- 공유 패키지는 역할별로 나눈다(`ui`·`config`·`api-client`·`auth`).
  - 단일 util 패키지를 두지 않는다 — 의존성 자석이 되어 모든 것이 흘러들어 응집도가 무너진다.

- 계층 2 — 앱과 공유 패키지 내부는 FSD-lite 레이어로 구성한다.

  | 레이어      | 담는 것                                                  | 의존 가능 대상    |
  | ----------- | -------------------------------------------------------- | ----------------- |
  | `app/`      | 라우팅·레이아웃·페이지 조립(컴포지션 루트)               | features 이하     |
  | `features/` | 사용자 행위 단위 수직 슬라이스                           | entities · shared |
  | `entities/` | 도메인 read-model·타입·Zod 스키마·순수 매퍼              | shared            |
  | `shared/`   | 기술 지원(ui·lib·config), 도메인 무지                    | 제로 도메인       |

- `features/`는 다른 `features/`를 import하지 않는다. 교차 feature 조립은 `app/`이 한다.
- FSD `widgets`·`pages` 레이어를 두지 않는다.
  - `app/`이 라우팅·페이지 조립 권한을 소유하므로 FSD `pages`와 충돌한다. element type에 등록하지 않아 그 레이어 사용을 린트가 거부한다.

- Turborepo + pnpm workspaces + FSD-lite를 채택한다.
  - 단일 앱을 기각한 이유: 경계가 리뷰로만 지켜져 조용히 썩는다. 패키지 경계 자체가 강제여야 타 도메인 참조가 안 샌다.
  - Nx를 기각한 이유: Turborepo가 캐싱·affected를 더 적은 config로 커버한다(코드젠·polyglot 필요 시 승격 대상).
  - 풀 FSD를 기각한 이유: 엔진(eslint-plugin-boundaries + dependency-cruiser)이 동일해 레이어 승격이 마이그레이션이 아니라 config 편집이므로, `widgets`·`pages` 모호성을 지고 시작할 이유가 없다. 가볍게 시작하는 비용이 0에 수렴한다.

### 앱 구성

- 앱은 배포 단위이고 레포에서 점차 늘어난다. 최소는 `apps/web` 하나다.

  | 앱      | 책임                 | 데이터 접근                                    |
  | ------- | -------------------- | ---------------------------------------------- |
  | `web`   | 공개 사용자 API·화면 | RSC 서버 페치·Server Action(경유는 api-client) |
  | `admin` | 어드민·백오피스      | 동일(권한·레이아웃만 격리)                     |

- 새 앱 추가는 워크스페이스 조립을 바꾼다. 아래 신규 슬라이스·패키지 체크리스트를 따른다.

### 세그먼트와 서버/클라 구조 경계

- 슬라이스는 세그먼트로 나눈다: `ui`(프레젠테이션)·`model`(상태·Server Action)·`api`(백엔드 호출). 서버/클라 경계는 슬라이스가 아니라 세그먼트에 떨어진다.
- 클라 코드는 `ui`에 두고 `index.client.ts`로 노출한다. 서버 전용 코드(`api`)는 `server-only`로 가드한다.
  - 서버/클라 누수는 `server-only`/`client-only` 포이즌 임포트가 빌드 에러로 막는다. 무엇을 서버·무엇을 클라에서 실행할지의 판단은 → [rendering](rendering.md)의 렌더 경계.
- 슬라이스 public API는 `index.client.ts`/`index.server.ts`로 RSC 라인을 따라 분리한다.
  - 서버/클라 라인을 가로지르는 혼합 배럴(`index.ts`)을 두지 않는다 — 클라 컴포넌트가 이를 import하면 서버 모듈이 클라 그래프로 끌려와 트리셰이킹·`server-only` 벽을 깬다.

### 시크릿 경계

- 서버 전용 값(API 키·세션 토큰·내부 URL)과 PII는 클라 번들·`NEXT_PUBLIC_*`·로그·에러 리포트에 노출하지 않는다. 서버 전용 코드는 `server-only`로 가드한다.
  - 로그·애널리틱스·에러 리포트는 관측 수단이 아니라 유출 표면이다.
- 클라에 노출할 값만 `NEXT_PUBLIC_` 접두를 붙인다. 접두 없는 env는 서버에서만 읽힌다(설정 배선은 아래 설정 불변식).

### read-model·api-client 배치

- 공유 도메인 모델은 `packages/entities`에, 앱 전용은 앱 내 `entities/`에 둔다(승격 기준은 위 2계층 모듈 지도). 이 모델이 담는 것·스키마·검증은 → [data](data.md)의 read-model.
  - 현재는 공유 소비처가 없어 read-model이 `apps/web`의 `entities/`에만 있고 `packages/entities`는 미구현이다. 둘째 소비 앱이 생길 때 승격한다.
  - 공유 계층에 둬도 도메인 로직·비즈니스 규칙을 originate하지 않는다(read-model의 성격은 → [data](data.md)의 read-model·BFF 경계).
- `packages/api-client`는 백엔드 타입드 클라이언트를 담고 `server-only`다. 호출·검증·auth 전파 규칙은 → [data](data.md)의 api-client.
- `shared`·`packages/*`에는 기술 지원만 둔다. 도메인 로직·도메인 지식을 넣지 않는다(방향은 빌드가 강제).

### `app/` 라우팅 트리 구조

- `app/`이 컴포지션 루트다. `page.tsx`는 얇게 — feature를 조립만 하고 도메인 로직을 두지 않는다(라우팅 행위는 → [rendering](rendering.md)의 라우팅, 라우트 상태 파일은 → [rendering](rendering.md)의 async 상태).
- 무런타임 조직 수단은 `app/` 구조로 둔다: route group `(group)`으로 URL 없는 묶음, private folder `_components`로 라우트 로컬 조립.
  - 런타임 의미·빌드 트랩을 동반하는 병렬·인터셉트 라우트는 → [rendering](rendering.md)의 라우팅이 소유한다.

### 설정 불변식

- 실행 앱은 아래 기본값을 전제한다. 다수 규칙의 전제다.
  - Node 런타임이 기본이다. Edge는 글로벌 저지연이 명확히 필요한 경로에만 `export const runtime = 'edge'`로 opt-in한다.
  - React Compiler는 Next 16에서 stable이나 기본 off·opt-in(`reactCompiler: true`)이다. 린트 전제는 → [code-quality](code-quality.md)의 ESLint.
  - env는 `packages/config`의 Zod 스키마로 부팅 시 검증한다. 시크릿은 환경변수로만 주입하고 파일에 커밋하지 않는다(클라 노출 경계는 위 시크릿 경계).
  - Pages Router를 두지 않는다(maintenance 모드). App Router + RSC가 기본이다.

### 공개 계약 호환성

- 공개 라우트·URL·쿼리파라미터·BFF 응답 형상은 호환성 경계다. 삭제·의미변경은 리다이렉트·deprecate를 경유한다.
  - 내부 App Router 경로 이동은 자유다 — 브라우저·외부가 소비하지 않는다.
  - 응답 형상 스펙(성공·에러·페이지네이션)은 → [data](data.md)의 응답 형상.

### 신규 슬라이스·패키지 체크리스트

- 새 feature 슬라이스: `features/{name}/`에 `ui`·`model`·`api` 세그먼트와 `index.client.ts`/`index.server.ts` public API를 만든다. feature→feature import를 만들지 않는다.
- 새 공유 패키지: `packages/{name}/`에 `package.json`(공개 export)·`tsconfig`·boundary element type 등록을 한다. 도메인 로직을 넣지 않는다.
- 새 앱: `apps/{name}/`에 App Router 구조를 만들고, 프론트 루트 `tsconfig.base.json` 상속과 앱 인라인 ESLint flat config를 배선한다(강제 기계 배치는 → [code-quality](code-quality.md)).

### 빌드가 강제하는 불변식

- 아래는 리뷰가 아니라 빌드가 막는 경계다. 코드 생성 후 이 목록으로 자기검증한다. 강제 장치가 아직 배선되지 않은 레포에서는 이 자기검증이 유일한 게이트다. 도구 설정·버전은 → [code-quality](code-quality.md).
  - 워크스페이스 방향(packages→apps 금지·앱 간 import 금지·패키지 순환·고아 모듈) — dependency-cruiser.
  - FSD 레이어 방향(feature→feature 금지·shared→상위 금지·public API 우회 깊은 import 금지) — eslint-plugin-boundaries.
  - 드롭한 레이어 거부(`widgets`·FSD `pages`가 element type에 없어 사용이 위반) — eslint-plugin-boundaries.
  - 서버/클라 누수(클라 모듈이 `api-client` 등 server-only 모듈을 import) — `server-only`/`client-only` 포이즌 임포트(빌드 에러). `auth` 도입 시 클라 훅 엔트리 사용은 정상이다(server-only core만 가드).
- 경계 도구 역할을 고정한다: dependency-cruiser=워크스페이스 방향, eslint-plugin-boundaries=FSD 레이어. 둘이 같은 관심사를 이중 집행하지 않는다.
  - steiger를 두지 않는다 — FSD 고정 taxonomy만 이해해 모노레포 `packages/` 승격을 오모델한다.

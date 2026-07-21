# Architecture

## 언제

- 새 앱·공유 패키지·feature 슬라이스를 추가할 때
- 모듈 경계와 의존 방향을 정할 때
- 서버/클라 구조 경계와 시크릿 위치를 정할 때
- 공유 도메인 read-model 배치 위치를 정할 때

## 규칙

### 2계층 모듈 구조

- 경계와 의존 방향은 빌드가 강제한다.
- 의존은 항상 한 방향으로만 흐른다.

#### 워크스페이스 계층

| 워크스페이스 | 역할 | 의존 가능 대상 |
|---|---|---|
| `apps/*` | 실행 앱 | 모든 `packages/*` |
| `packages/entities` | 공유 read-model·타입·Zod 스키마 | 하위 패키지 |
| `packages/ui` | 디자인시스템 | 하위 패키지 |
| `packages/api-client` | 타입드 클라이언트(server-only) | 하위 패키지 |
| `packages/auth` | 인증 코어·클라 훅 분리 | 하위 패키지 |
| `packages/config` | 공용 설정·상수·env 스키마 | 없음 |

- `apps/* → packages/*`만 허용한다.
- `packages → apps`, `app ↔ app`, 패키지 순환은 금지한다.
- `packages/entities`는 `ui`, `api-client`, `auth`에 의존하지 않는다.
- 공유 기준:
  - 두 앱 이상 사용 → `packages/*`
  - 단일 앱 전용 → 앱 내부 `src/`
  - 애매하면 앱 로컬에 두고 두 번째 사용처가 생기면 승격한다.
- `ui`, `config`, `api-client`, `auth`처럼 역할별 패키지로 분리한다.
- 범용 util 패키지는 금지한다.
- 공통 코드는 책임별 패키지 또는 `shared` 내부 역할 단위로 둔다.

#### FSD-lite 계층

| 레이어 | 역할 | 의존 가능 대상 |
|---|---|---|
| `app/` | 라우팅·레이아웃·페이지 조립(컴포지션 루트) | `features` |
| `features/` | 사용자 행위 단위 | `entities`, `shared` |
| `entities/` | read-model·타입·스키마·순수 매퍼 | `shared` |
| `shared/` | 기술 지원(ui·lib·config), 도메인 무지 | 없음 |

- `app`은 feature 조립을 담당한다.
- `features` 간 직접 import는 금지한다.
- 교차 feature 조립은 `app/`이 담당한다.
- `widgets`, FSD `pages` 레이어는 사용하지 않는다.
- FSD는 lite 형태만 사용한다.
- 레이어 확장은 경계 설정 변경으로만 허용한다.

### 앱 구성

| 앱 | 책임 | 데이터 접근 |
|---|---|---|
| `web` | 공개 사용자 화면·API | RSC·Server Action |
| `admin` | 어드민·백오피스 | 동일 |

- 앱은 배포 단위다.
- 새 앱 추가 시 워크스페이스 조립만 변경한다.

### 세그먼트 구조와 서버/클라 경계

- 슬라이스는 `ui`, `model`, `api` 세그먼트로 분리한다.
- 클라 코드는 `ui`에 두고 `index.client.ts`로 공개한다.
- 서버 전용 코드는 `api`에 두고 `server-only`로 가드한다.
- 공개 API는 `index.client.ts`, `index.server.ts`로 분리한다.
- 서버·클라 혼합 배럴(`index.ts`)은 금지한다.
- 클라 import 시 서버 모듈이 클라 그래프에 포함되지 않도록 분리한다.
- `server-only`, `client-only` 포이즌 임포트로 서버·클라 누수를 빌드 에러로 차단한다.

### 서버 경계

- API 키, 세션 토큰, 내부 URL, PII를 클라 번들·`NEXT_PUBLIC_*`·로그에 노출하지 않는다.
- 클라 공개 값만 `NEXT_PUBLIC_` 접두사를 사용한다.
- 공유 read-model → `packages/entities`
- 앱 전용 read-model → 앱 내부 `entities/`
- `packages/api-client`는 인증 컨텍스트를 전파하는 `server-only` 타입드 클라이언트다.
- 호출·검증·auth 전파 규칙은 api-client 책임이다.
- `shared`, `packages/*`에는 도메인 로직을 두지 않는다.
- 공유 계층은 도메인 규칙의 원천이 아니다.
- read-model은 조회 표현과 검증 계약만 담당한다.

### app 라우팅 구조

- `app/`이 컴포지션 루트다.
- `page.tsx`는 얇게 유지한다.
- feature 조립만 하고 도메인 로직은 두지 않는다.
- URL 없는 그룹은 `(group)`으로 구성한다.
- 라우트 전용 조립 코드는 `_components`에 둔다.

### 설정 불변식

- 기본 런타임은 Node다.
- Edge는 글로벌 저지연이 명확히 필요한 경로에서만 `runtime = 'edge'`로 opt-in한다.
- React Compiler는 Next 16에서 stable이며 기본은 off다. `reactCompiler: true`로 opt-in한다.
- env는 `packages/config`의 Zod 스키마로 부팅 시 검증한다.
- 시크릿은 환경변수로만 주입한다.
- Pages Router는 사용하지 않는다.

### 기술 선택 기준

- Turborepo + pnpm workspaces를 기본으로 한다.
- Nx는 코드젠·polyglot 요구가 생길 때 검토한다.
- FSD는 lite 형태만 사용한다.
- `widgets`, `pages` 레이어는 두지 않는다.

### 공개 계약

- 공개 URL, 쿼리파라미터, BFF의 성공·에러·페이지네이션 응답 형상은 호환성 경계다.
- 삭제·의미 변경은 리다이렉트 또는 deprecate를 거친다.
- 내부 App Router 경로 이동은 자유롭게 허용한다.

### 신규 슬라이스 체크리스트

- `features/{name}/ui`
- `features/{name}/model`
- `features/{name}/api`
- `index.client.ts`
- `index.server.ts`
- feature 간 직접 import 없음

### 신규 패키지 체크리스트

- `packages/{name}/`
- `package.json` export 정의
- `tsconfig`
- boundary element type 등록
- 도메인 로직 없음

### 신규 앱 체크리스트

- `apps/{name}/`
- App Router 구조
- 공용 `tsconfig` 상속
- 공용 `eslint-config` 상속

### 빌드 불변식

| 도구 | 강제 대상 |
|---|---|
| `dependency-cruiser` | 워크스페이스 방향 |
| `eslint-plugin-boundaries` | FSD 레이어 |
| `server-only` / `client-only` | 서버·클라 경계 |

#### dependency-cruiser

- 워크스페이스 방향 위반 금지
- 앱 간 import 금지
- 패키지 순환 금지
- 고아 모듈 금지

#### eslint-plugin-boundaries

- feature→feature 금지
- shared→상위 레이어 금지
- public API 우회 deep import 금지
- `widgets`, FSD `pages` 사용 금지

#### poison import

- 클라에서 `api-client` import 금지
- 클라에서 `auth`의 server-only 코어 import 금지

- 같은 관심사를 중복 검사하지 않는다.
- `steiger`는 사용하지 않는다.
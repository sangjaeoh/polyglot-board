# Data

## 언제

- 프론트 도메인 모델(read-model)을 확정할 때.
- 데이터 페칭·상태 소유·캐시·변경 전략을 결정할 때.
- BFF(route handler·Server Action)의 API 집약·조율, 응답·에러·페이지네이션 계약을 정의할 때.
- 폼을 구현할 때.
  - 렌더 위치·async 상태는 [rendering](rendering.md)을 따른다.

## 규칙

### read-model

- 프론트 도메인 모델은 백엔드 진실의 read-model이다.
- read-model을 진실 원천으로 사용하지 않는다.
- 백엔드가 거부할 불변식은 read-model에서 강제하지 않는다.
- 진실 소유는 백엔드다.
- read-model은 백엔드 데이터의 투영이며 Zod 스키마와 순수 뷰 파생(포맷·표시 계산)만 가진다.
- 타입 파생은 → [coding-conventions](coding-conventions.md)의 타입을 따른다.
- 클라이언트 전달 뷰모델은 직렬화 가능하고 최소화한다.
- read-model 배치는 → [architecture](architecture.md)의 서버 경계를 따른다.

### 데이터 흐름

- 기본 데이터 흐름은 서버다.
  - 읽기: RSC 서버 페치 또는 route handler(GET).
  - 변경: Server Action.
- TanStack Query는 서버 상태 캐시 동기화 용도로만 사용한다.
  - 앱 상태 저장소로 사용하지 않는다.
  - `queryFn`은 route handler(GET)를 호출한다.
  - Server Action을 조회 페치 용도로 사용하지 않는다.
    - POST·내비게이션 단위 실행으로 직렬화 지연을 만든다.
- 독립 데이터는 서버에서 병렬 조회한다(`Promise.all`).
- 순차 `await` 체인으로 워터폴을 만들지 않는다.
- 느린 소스는 Suspense 경계로 분리해 스트리밍한다.
- 동일 요청 내 동일 데이터는 React `cache()`로 메모이즈한다.
- 목록 항목별 개별 페치는 금지한다.
  - 서버에서 배치 조회해 클라이언트 N+1을 방지한다.
- 클라이언트 상태는 URL·searchParams를 우선한다.
- 전역 상태는 Zustand를 최소 사용한다.
- 클라이언트 상태는 상호작용 상태만 가진다.
- 서버 상태를 클라이언트 스토어에 미러링하지 않는다.
  - 복제하면 동기화 시점 결합이 생긴다.
  - 읽는 시점의 값이 이미 변경될 수 있으므로 서버 상태는 RSC·Query가 소유한다.
- Redux·XState 같은 무거운 상태 라이브러리는 baseline이 아니다.
  - URL + Query + Zustand로 부족한 경우만 opt-in한다.

### BFF 경계

- BFF는 데이터 shape·join·filter·cache를 수행할 수 있다.
- 백엔드가 소유해야 할 비즈니스 규칙은 생성하지 않는다.
- BFF 비즈니스 규칙은 백엔드와 규칙 drift를 만든다.
- 크로스 소스 조립은 서버에서 병렬 집약한다.

### api-client

#### 호출 경계

- 백엔드 호출은 `packages/api-client`의 타입드 클라이언트(server-only)만 사용한다.
- 컴포넌트·핸들러에 raw fetch 문자열을 분산하지 않는다.
- 인터페이스와 구현을 분리한다.
- 백엔드 교체·MSA 분리 시 소비처 수정 없이 교체 가능해야 한다.
- 클라이언트 타입·이름에 백엔드 벤더·URL을 노출하지 않는다.
- auth는 세션에서 추출해 요청에 전파한다.
- 시크릿·내부 URL은 server-only 경계에 둔다.

#### 응답 검증

- egress 검증은 api-client 경계 1곳에서 `safeParse`한다.
- 기본 egress 실패는 국소 degrade한다.
- 계약 변경은 코드젠·타입 컴파일에서 드러난다.
- 핫 RSC 경로마다 `.parse`를 반복하지 않는다.
- 비용이 큰 경로는 샘플링 검증한다.
- ingress와 egress 검증 강도는 다르게 적용한다.
  - ingress: 적대적 입력이므로 항상 엄격 검증한다.
  - egress: 1st-party 계약 기반이므로 경계 1곳 검증한다.

#### 목록 egress

- 페이지 봉투(pagination meta)를 먼저 검증한다.
- 봉투 실패는 throw한다.
- 항목 배열은 개별 `safeParse`한다.
- 불량 항목은 드롭하고 서버 로그를 남긴다.
- 드롭 시 pagination meta와 항목 수가 불일치한다.
- 정합성이 중요한 화면은 드롭하지 않고 throw한다.

#### 상세 egress

- 단일 리소스 egress 실패는 타입드 에러로 throw한다.
- 단일 리소스는 드롭 가능한 국소 단위가 없다.
- 라우트 에러 바운더리에서 격리되므로 throw 자체가 국소 degrade다.

#### API 방식

- 백엔드가 별도 서비스면 tRPC를 사용하지 않는다.
- 코드젠 + Zod 기반 타입 클라이언트로 배포 결합 없이 계약을 유지한다.
- 백엔드가 TS 모노레포로 통합되면 재검토한다.

### 캐시

- 캐시 태그 taxonomy는 대상 read-model을 소유한 entities 계층이 상수로 소유한다.
- per-user 캐시와 shared 캐시 태그를 분리한다.
- auth 스코프 데이터를 shared 캐시로 저장하지 않는다.
- 무효화 소유자를 하나로 둔다.
- Next 16 캐시는 기본 uncached를 전제로 한다.
  - `fetch`와 GET route handler는 기본 uncached다.
- 세그먼트 캐싱은 `use cache` + `cacheTag`/`cacheLife`를 사용한다.
  - `cacheComponents` 플래그가 필요하다.
- `revalidateTag(tag, profile)`을 사용한다.
  - 단일 인자 `revalidateTag(tag)`는 사용하지 않는다.
- 사용자 변경 즉시 반영이 필요하면 Server Action에서 `updateTag`를 사용한다.
- Next Cache·CDN·Query 중 revalidation 진실 소유자를 하나로 둔다.

### 응답 형상

#### 입력 검증

- ingress(route 파라미터·Server Action 입력)는 엄격 Zod 검증한다.
- 상태 변경 입력은 보정 없이 거부한다.
- 예외: 상태를 변경하지 않는 복구 가능한 표시 파라미터(page 등)는 `.catch` 기본값 보정을 허용한다.

#### 성공 응답

- 스칼라·객체 응답은 envelope 없이 반환한다.
- 리스트 응답은 pagination meta를 포함한다.
- 기본 페이지네이션은 offset이다.
  - 총개수·`page`·`pageSize` 포함.
- 커서·무한스크롤은 명시적으로 선택한다.
  - meta에 `nextCursor`·`hasMore` 포함.

#### 에러 처리

- 백엔드 ProblemDetail을 타입드 에러로 매핑한다.
- 타입드 에러 타입은 `packages/entities`가 소유한다.
- 에러 UI·바운더리는 타입드 에러를 소비한다.
- 무타입·애드혹 에러 처리는 금지한다.
- RFC 9457 ProblemDetail을 BFF 경계에서 타입드 에러로 고정한다.

### Server Action

- Server Action은 `'use server'` async 함수다.
- 변경 전용으로 사용한다.
- 인가는 함수 본문에서 판정한다.
- 렌더 컨텍스트는 보안 경계가 아니다.
- Server Action은 공개 POST 엔드포인트로 취급한다.
- 시크릿을 Server Action 클로저로 캡처하지 않는다.
  - 캡처 값은 클라이언트 전달 payload 대상이 될 수 있다.

### 폼

- 폼 모델은 하이브리드로 선택한다.

| 조건 | 선택 |
|---|---|
| Server Action 제출 | `useActionState` + Conform |
| 리치 클라이언트 폼 | react-hook-form |

- 동일 Zod 스키마를 클라이언트·서버에서 재사용한다.
- Conform은 서버 ingress 검증과 필드 에러 처리를 담당한다.
- 서버 재검증은 → 입력 검증을 따른다.

### 변경 안전

- 변경 요청에 클라이언트 생성 멱등키를 포함한다.
- 중복 요청 처리는 백엔드가 담당한다.
- 도메인 ID는 백엔드가 소유한다.
- 프론트는 멱등키만 생성한다.
- 낙관적 업데이트는 필요할 때만 사용한다.
  - 반영 → Server Action → 실패 시 롤백 → 성공 후 `updateTag` 재동기화.
- 롤백 없는 낙관적 반영은 금지한다.
- 오프라인 큐·PWA는 baseline이 아니다.
  - 필요 시 멱등키 기반 재전송 안전성을 확보해 opt-in한다.
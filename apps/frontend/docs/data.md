# Data

## 언제

- 프론트 도메인 모델(read-model)을 확정할 때. 데이터 페칭·상태 소유·캐시·변경(mutation) 전략을 정할 때.
- BFF(route handler·Server Action)가 백엔드 API를 어떻게 집약·조율할지, 응답·에러·페이지네이션 계약을 정할 때.
- 폼을 구현할 때. 렌더 위치·async 상태 자체는 → [rendering](rendering.md).

## 규칙

### read-model

- 프론트 도메인 모델은 백엔드 진실의 read-model이다. 진실 원천이 아니다.
  - 백엔드가 거부할 불변식을 여기서 강제하지 않는다. 진실 소유는 백엔드에 있고 프론트는 그 투영을 읽는다.
- read-model이 담는 것: Zod 스키마와 순수 뷰 파생(포맷·표시용 계산). 타입은 스키마에서 `z.infer`로 파생한다 → [coding-conventions](coding-conventions.md)의 타입.
- 클라로 넘길 뷰모델은 직렬화 가능·최소여야 한다 → [rendering](rendering.md)의 렌더 경계.
- 배치(`packages/entities` vs 앱 로컬)는 → [architecture](architecture.md)의 read-model·api-client 배치.

### 데이터 흐름

- 데이터 흐름의 기본값은 서버다. 읽기는 RSC 서버 페치 또는 route handler(GET), 변경은 Server Action이다.
- 클라 서버상태는 TanStack Query가 서버캐시 동기화 한정으로 소유한다 — 앱 상태 저장소로 쓰지 않는다.
  - Query의 `queryFn`은 route handler(GET)를 호출한다. Server Action을 페치로 쓰지 않는다 — POST·내비게이션당 순차 실행이라 직렬화 지연을 먹는다.
- 독립 페치는 서버에서 병렬로 모은다(`Promise.all`). 순차 `await` 체인(워터폴)을 만들지 않는다.
  - 느린 소스는 Suspense 경계로 분리해 스트리밍한다 → [rendering](rendering.md)의 렌더 전략.
- 여러 컴포넌트가 같은 데이터를 필요로 하면 React `cache()`로 요청 단위 메모이즈한다.
- 목록 항목마다 개별 페치를 걸지 않는다 — 서버에서 배치 조회로 집약한다(클라 N+1 회피).
- 클라 상태는 URL·searchParams를 우선하고, 전역은 Zustand를 최소로 쓴다.
  - 서버상태를 클라 스토어에 미러링하지 않는다. 복제하면 동기화 시점 결합이 생긴다 — 읽는 시점의 값이 이미 바뀌었을 수 있다. 그래서 서버상태는 RSC·Query가 소유하고 클라는 상호작용 상태만 갖는다.
  - Redux·XState 같은 무거운 상태 라이브러리는 baseline이 아니다 — URL + Query + Zustand로 부족할 때 opt-in한다.

### BFF 경계

- BFF는 데이터를 shape·join·filter·cache 할 수 있으나, 백엔드가 거부하지 않을 규칙을 originate 하면 안 된다.
  - 어떤 비즈니스 결정이 오직 BFF에만 있으면 그 결정은 잘못된 계층에 있다. 이 리트머스가 지켜지지 않으면 누군가 "최적화"로 로직을 프론트로 당겨 백엔드와 규칙이 드리프트한다. BFF는 이것이 죽기 쉬운 곳이다(집약→"조금만" 재형→"규칙 하나").
- 크로스 소스 조립은 서버에서 병렬로 집약한다(위 데이터 흐름).

### api-client

- 백엔드 호출은 `packages/api-client`의 타입드 클라이언트(server-only)로만 한다. raw fetch 문자열을 컴포넌트·핸들러에 흩지 않는다.
  - 인터페이스와 구현이 분리되면 백엔드 교체·MSA 분리 시 소비처를 무수정으로 갈아끼운다. 클라이언트는 벤더 중립이다 — 백엔드 벤더·URL을 타입·이름에 노출하지 않는다.
- auth를 세션에서 꺼내 요청에 전파한다. 시크릿·내부 URL은 server-only 경계 안에 둔다 → [architecture](architecture.md)의 시크릿 경계.
- egress(백엔드→BFF) 검증은 이 경계 1곳에서 `safeParse`한다. 실패는 국소 degrade한다 — 페이지 전체를 throw로 만들지 않는다.
  - ingress(클라→BFF)와 egress는 비대칭 위협이다. ingress는 적대적이라 항상 엄격 검증하고, egress는 계약에서 코드젠된 1st-party라 경계 1곳만 검증한다. 핫 RSC 경로마다 `.parse`를 흩으면 렌더 지연·TTFB만 먹고, 계약 변경은 코드젠·타입 컴파일에서 드러난다. 비용 큰 경로는 샘플링한다.
- 목록 egress는 페이지 봉투(pagination meta)를 검증한 뒤 항목 배열을 개별 `safeParse`한다. 불량 항목은 드롭하고 서버 로그로 남긴다.
  - 봉투가 깨지면 목록 자체를 신뢰할 수 없으니 throw하고, 항목 하나의 계약 위반은 그 항목만 드롭한다 — 레코드 하나가 페이지 전체를 죽이면 국소 degrade가 아니다.
  - 드롭은 pagination meta와 어긋난다(총개수는 그대로인데 항목이 빠진다) — 이 불일치가 드롭의 대가다. 정합이 우선인 화면(정산·어드민 류)은 드롭 대신 throw를 택한다.
- 상세(단일 리소스) egress 실패는 타입드 에러로 throw한다 — 국소 degrade 규칙의 예외다.
  - 리소스 하나가 응답 전부라 드롭할 국소 단위가 없고, 에러 바운더리가 해당 라우트만 격리하므로 throw가 곧 국소 degrade다.
- 백엔드가 별도 서비스라 tRPC는 두지 않는다 — 코드젠 + Zod가 배포 결합 없이 타입 클라이언트를 준다(백엔드가 TS 모노레포로 합쳐지면 재검토).

### 캐시

- 기본 캐시 전략은 no-store + `revalidatePath`다: 백엔드 페치는 uncached(`cache: 'no-store'`)로 두고, 변경 후 Server Action이 영향 경로를 `revalidatePath`로 무효화한다. 태그 기반 캐싱(`use cache`·`cacheTag`·`revalidateTag`·`updateTag`)은 이 기본으로 부족할 때만 도입한다.
  - 태그 기반을 도입하면 per-user 캐시와 shared 캐시를 태그로 분리하고 무효화 소유자를 하나로 둔다 — auth 스코프 데이터를 shared로 캐싱하면 사용자 A가 B의 데이터를 본다.
- Next 16 캐시 기본값을 전제한다: `fetch`와 GET route handler는 기본 uncached다.
- Next Cache·CDN·Query 중 revalidation 진실 소유자를 하나로 둔다. 셋이 같은 데이터를 다르게 무효화하지 않게 한다.

### 응답 형상

- ingress(route 파라미터·Server Action 입력)는 항상 엄격 Zod로 검증한다(적대적 입력).
  - 예외: 상태를 바꾸지 않는 복구 가능한 표시 파라미터(page 등)는 `.catch`로 기본값 보정을 허용한다. 상태를 바꾸는 입력은 보정 없이 거부한다.
- 성공 응답은 스칼라·객체는 envelope 없이 형상 그대로, 리스트는 pagination meta를 동반한다. 기본은 오프셋(총개수·`page`·`pageSize`)이고, 커서·무한스크롤은 명시하며 meta에 `nextCursor`/`hasMore`를 싣는다.
- 에러는 백엔드 ProblemDetail을 타입드 에러로 매핑한다. 에러 UI·바운더리(→ [rendering](rendering.md)의 async 상태)가 이를 소비한다.
  - 무타입·애드혹 에러 처리를 기각한 이유: 상태 누락·비일관 UX. RFC 9457 ProblemDetail을 BFF 경계에서 타입드 에러로 고정한다.
- 공개 계약의 호환성·안정성 규칙은 → [architecture](architecture.md)의 공개 계약 호환성. 여기선 형상 스펙만 소유한다.
- Server Action은 `'use server'`로 표시한 async 함수다(파일 상단 지시어 또는 함수 내 인라인). 변경 전용이며 읽기·페치에 쓰지 않는다.
- 인가는 모든 서버 데이터 경계(RSC 서버 페치·route handler·Server Action)의 함수 본문 안에서 판정한다. 렌더 컨텍스트(버튼 미표시)는 보안 경계가 아니다.
  - 특히 Server Action은 공개 POST 엔드포인트라 로컬 호출처럼 보여도 경계 밖에서 직접 호출될 수 있다 — 반드시 본문에서 재인가한다.
- 시크릿을 Server Action 클로저로 캡처하지 않는다 — 캡처 변수는 암호화 payload에 실린다.

### 폼

- 폼 모델은 하이브리드다. 분할 규칙은 기계적이다: Server Action으로 제출하면 `useActionState` + Conform, 리치 클라 전용(위저드·동적 필드·watch 조건 렌더)이면 react-hook-form.
  - 같은 Zod 스키마를 클라·서버 양쪽에서 재사용한다. Conform은 서버 ingress 검증과 클라 필드 에러를 겸하고 JS 전에도 HTML POST로 동작하나, 리치 상호작용에서는 싸운다. 단일 패러다임 강제를 기각한 이유: Conform 전면은 리치 폼에서, RHF 전면은 점진적 향상에서 각각 무너진다.
- Server Action 입력은 서버에서 다시 엄격 검증한다(위 응답 형상의 ingress).

### 변경 안전

- 변경 요청에 클라 생성 멱등키를 싣는다 — 같은 키의 재전송을 백엔드가 중복 없이 처리한다. 도메인 ID는 백엔드가 소유한다(프론트는 멱등키만 만든다).
- 낙관적 업데이트가 필요할 때만 서버상태를 클라로 이관한다: 반영 → Server Action → 실패 시 롤백을 명시적으로 구현한다. 성공 후 재검증(기본은 `revalidatePath`, 위 캐시)으로 서버 진실과 재동기화한다.
  - 롤백 경로 없는 낙관적 반영을 만들지 않는다. 오프라인 큐·PWA는 baseline이 아니다 — 필요 시 멱등키로 재전송 안전을 확보해 opt-in한다.

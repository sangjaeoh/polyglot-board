# Rendering

## 언제

- 무엇을 서버에서·무엇을 클라에서 렌더할지, 렌더 전략(정적·동적·스트리밍)을 정할 때.
- App Router 라우트(`page`·`layout`·병렬·인터셉트)와 라우트 상태 파일을 만들거나 고칠 때.
- 메타데이터를 붙일 때. 데이터 페칭·캐시·계약 자체는 → [data](data.md).

## 규칙

### 렌더 경계

- 기본은 서버다. 컴포넌트는 RSC로 시작하고 `'use client'`는 상호작용이 필요한 leaf까지 밀어 내린다. 경계가 구조적으로 어디 떨어지는지는 → [architecture](architecture.md)의 세그먼트와 서버/클라 구조 경계.
  - 렌더 경계는 이 스택의 핵심 구조 seam이다. 백엔드의 스키마 경계가 네트워크를 못 넘듯, 서버/클라 경계는 브라우저로 넘어가는 것을 결정하는 계약이다. 모든 `'use client'`는 번들 비용이자 직렬화 경계이자 시크릿 유출 표면이므로, 기본을 서버로 두고 상호작용만 leaf로 내린다.
- 클라로 넘기는 것(props)은 직렬화 가능·최소·Zod 검증된 안정 계약이다. 함수·클래스 인스턴스·서버 전용 객체를 넘기지 않는다(클라가 받는 뷰모델은 → [data](data.md)의 read-model).
- 서버 전용 값·시크릿을 클라 컴포넌트 props로 내리지 않는다 → [architecture](architecture.md)의 시크릿 경계.

### hydration 결정성

- 서버/클라 렌더가 발산하지 않게 한다. `Date.now()`·`Math.random()`·locale·`window`·`localStorage` 의존을 서버 렌더 경로에서 쓰지 않는다.
  - hydration mismatch는 조용한 시각 버그를 만든다. 시각·랜덤·로케일 의존 값은 클라 effect에서 채우거나 서버에서 확정해 props로 내린다.
- `suppressHydrationWarning`을 근본 원인을 가리는 용도로 쓰지 않는다.

### 렌더 전략

- 렌더 전략을 명시적으로 택한다.
  - 정적(빌드 시) — 사용자·요청 무관 콘텐츠. 동적 세그먼트(`[id]`)를 빌드 시 프리렌더하려면 `generateStaticParams`로 경로를 미리 만든다.
  - 동적(요청 시) — 사용자·요청 의존.
  - 스트리밍 — 느린 데이터를 Suspense 경계로 나눠 빠른 셸을 먼저 렌더하고 느린 조각을 흘린다.
- 정적/동적을 한 라우트에 섞는 최적화는 baseline이 아니다 — 필요할 때 Cache Components(`cacheComponents` + `use cache`)로 표현한다 → [data](data.md)의 캐시.
  - 구 `experimental.ppr` 플래그와 route별 PPR export는 Next 16에서 제거됐다. 정적/동적 혼합은 이제 Cache Components 모델로 표현한다.

### 라우팅

- `app/`이 컴포지션 루트이고 `page.tsx`는 얇다(구조·조직 수단은 → [architecture](architecture.md)의 `app/` 라우팅 트리 구조). 여기선 라우트의 런타임 행위만 다룬다.
- 내부 내비게이션은 `next/link`로 한다. `<a href>`로 내부 경로를 이동하면 클라 라우팅을 건너뛰어 풀 리로드가 된다.
- `params`·`searchParams`·`cookies()`·`headers()`·`draftMode()`는 Promise다 — `await`로 읽는다(Next 16). 동기 접근은 실패한다: `cookies()`·`headers()`는 throw, `params`·`searchParams`는 미await 시 조용히 `undefined`가 된다.
- 병렬 라우트(`@slot`)·인터셉트 라우트(`(.)`·`(..)`)는 모달·독립 로딩 슬롯에 쓴다.
  - 병렬 라우트 슬롯마다 `default.tsx`를 둔다 — 없으면 Next 16 빌드가 실패한다. 슬롯이 매칭 안 되면 `null` 또는 `notFound()`를 반환한다.
- 인가 판정은 → [data](data.md)의 응답 형상(재인가). 라우트 진입의 미인가·부재는 `redirect()`·`notFound()`로 처리한다.
  - `redirect()`·`notFound()`는 throw로 렌더를 중단한다 — try/catch로 삼키면 처리가 무력화된다.
- 요청 가로채기는 `proxy`(`proxy.ts`, Node 런타임 전용)로 한다. 구 `middleware`는 Next 16에서 deprecated다(Edge 용도로는 아직 동작하나 경고를 낸다).
  - `middleware.ts`를 `proxy.ts`로 옮기면 런타임이 Node로 바뀐다 — Edge에 의존하던 로직(지역 분산·Edge 전용 API)은 단순 리네임으로 끝나지 않고 재작업이 필요하다.
  - 가로채기는 최후 수단이다. 인가는 라우트·route handler·Server Action 내부에서 판정한다 → [data](data.md)의 응답 형상(재인가).

### async 상태

- 모든 async·데이터 표면은 loading·error·empty 상태를 명시적으로 갖는다. 기본 케이스가 아니라 필수다.
- 라우트 상태 파일을 목적에 맞게 쓴다: `loading.tsx`(스트리밍 fallback) · `error.tsx`(세그먼트 에러 바운더리) · `not-found.tsx`(404) · `global-error.tsx`(루트 레이아웃 에러).
  - `error.tsx`·`global-error.tsx`는 `'use client'` 컴포넌트여야 한다 — 에러 바운더리는 클라에서 렌더된다.
- `error.tsx`는 부분 실패(한 섹션)를 페이지 전체 크래시로 만들지 않는다 — 국소 degrade한다. 에러 바운더리가 소비하는 에러 계약은 → [data](data.md)의 응답 형상(ProblemDetail).
- empty 상태를 의미 있게 설계한다(빈 목록·검색 무결과). 침묵하지 않는다.

### 메타데이터

- 메타데이터는 Next 내장 Metadata API로 선언한다. `next-seo` 등 별도 라이브러리를 도입하지 않는다.
- 요청·데이터 무관 메타는 정적 `metadata`로, 데이터 의존 메타는 동적 `generateMetadata`로 선언한다. `generateMetadata`는 서버에서 실행되고 `params`를 await한다.
- 루트 레이아웃 `metadata`에 `metadataBase`를 설정한다 — 없으면 OG·canonical의 상대 URL이 localhost로 해석된다.
- viewport·themeColor는 `metadata`가 아니라 별도 `viewport` export로 선언한다(Next 14+).
- fetch 데이터는 렌더 패스에서 요청 단위로 메모이즈되어 페이지·메타가 같은 fetch를 공유한다. 비-fetch 접근의 요청 단위 메모이즈는 `cache()`로 한다(→ [data](data.md)의 데이터 흐름). 지속 캐시는 → [data](data.md)의 캐시.

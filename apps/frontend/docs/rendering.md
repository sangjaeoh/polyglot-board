# Rendering

## 언제

- 서버·클라이언트 렌더 위치와 렌더 전략(정적·동적·스트리밍)을 결정할 때.
- App Router 라우트(`page`·`layout`·병렬·인터셉트)와 상태 파일을 생성·수정할 때.
- 메타데이터를 선언할 때.

## 렌더 경계

- 기본 렌더 대상은 서버 컴포넌트(RSC)다.
- `'use client'`는 상호작용이 필요한 leaf 컴포넌트까지 최소 범위로 내린다.
- 렌더 경계는 서버/클라이언트 계약 경계다. 모든 `'use client'`는 번들 비용·직렬화 경계·시크릿 노출 표면을 만든다.
- 클라이언트 props는 직렬화 가능·최소·Zod 검증된 안정 계약만 전달한다.
- 함수·클래스 인스턴스·서버 전용 객체를 클라이언트 props로 전달하지 않는다.
- 클라이언트 뷰모델은 → [data](data.md)의 read-model 규칙을 따른다.
- 서버 전용 값·시크릿을 클라이언트로 전달하지 않는다.

## Hydration 결정성

- 서버와 클라이언트 렌더 결과가 일치해야 한다.
- hydration mismatch는 조용한 시각 버그를 만든다.
- 서버 렌더 경로에서 비결정 값을 사용하지 않는다.
  - `Date.now()`
  - `Math.random()`
  - locale 의존 값
  - `window`
  - `localStorage`
- 시각·랜덤·로케일 값은 클라이언트 effect에서 생성하거나 서버에서 확정 후 props로 전달한다.
- `suppressHydrationWarning`은 원인 제거 없이 사용하지 않는다.

## 렌더 전략

- 렌더 전략은 사용자·요청 의존성 기준으로 결정한다. 데이터 특성만으로 정하지 않는다.

| 전략 | 조건 |
|---|---|
| 정적 렌더 | 사용자·요청과 무관한 콘텐츠 |
| 동적 렌더 | 사용자·요청 의존 콘텐츠 |
| 스트리밍 | 느린 데이터를 Suspense 경계로 분리 |

- 동적 세그먼트(`[id]`)를 빌드 시 생성할 때 `generateStaticParams`로 경로를 정의한다.
- 정적·동적 혼합 최적화는 기본 전략이 아니다.
- 혼합 렌더링은 필요할 때 Cache Components(`cacheComponents` + `use cache`) 모델로 표현한다.
- 구 `experimental.ppr` 플래그와 route별 PPR export는 사용하지 않는다.

## 라우팅

- `app/`은 컴포지션 루트다.
- `page.tsx`는 얇게 유지한다.
- 내부 이동은 `next/link`를 사용한다.
- 내부 경로 이동에 `<a href>`를 사용하지 않는다.
- `params`·`searchParams`·`cookies()`·`headers()`·`draftMode()`는 `await` 후 사용한다.
- `cookies()`·`headers()`의 동기 접근은 실패한다.
- `params`·`searchParams`의 미대기는 잘못된 값으로 처리될 수 있다.

### 병렬·인터셉트 라우트

- 병렬 라우트(`@slot`)는 독립 슬롯·모달 구성에 사용한다.
- 인터셉트 라우트(`(.)`·`(..)`)는 라우트 가로채기에 사용한다.
- 병렬 슬롯마다 `default.tsx`를 둔다.
- 매칭되지 않은 슬롯은 `null` 또는 `notFound()`를 반환한다.

### 접근 제어·요청 가로채기

- 라우트 진입의 미인가·부재는 `redirect()`·`notFound()`로 처리한다.
- `redirect()`·`notFound()`는 throw 기반 종료다.
- `redirect()`·`notFound()`를 `try/catch`로 삼키지 않는다.
- 요청 가로채기는 `proxy.ts`에서 처리한다.
- `middleware.ts`는 deprecated다.
- `proxy.ts`는 Node 런타임 전용이다.
- `middleware.ts`에서 `proxy.ts`로 변경 시 Edge 의존 로직을 재검토한다.
- 가로채기는 최후 수단이다.
- 인가는 라우트·route handler·Server Action 내부에서 우선 판단한다.

## Async 상태

- 모든 async·데이터 UI는 loading·error·empty 상태를 필수로 가진다.

| 파일 | 역할 |
|---|---|
| `loading.tsx` | 스트리밍 fallback |
| `error.tsx` | 세그먼트 에러 바운더리 |
| `not-found.tsx` | 404 처리 |
| `global-error.tsx` | 루트 레이아웃 에러 |

- `error.tsx`·`global-error.tsx`는 `'use client'` 컴포넌트다.
- `error.tsx`는 부분 실패를 국소 처리한다.
- 에러 계약은 → [data](data.md)의 ProblemDetail 규칙을 따른다.
- empty 상태는 빈 목록·검색 결과 없음 등을 의미 있게 표현한다.

## 메타데이터

- Metadata API로 메타데이터를 선언한다.
- 별도 SEO 라이브러리를 도입하지 않는다.

| 대상 | 선언 방식 |
|---|---|
| 요청·데이터 무관 메타 | `metadata` |
| 데이터 의존 메타 | `generateMetadata` |

- `generateMetadata`는 서버에서 실행한다.
- `generateMetadata`에서 `params`를 await한다.
- 루트 레이아웃에 `metadataBase`를 설정한다. OG·canonical 상대 URL 오류를 방지한다.
- `viewport`·`themeColor`는 `viewport` export로 선언한다.
- `metadata`에 `viewport`·`themeColor`를 선언하지 않는다.
- fetch 데이터는 렌더 패스 내 요청 단위로 메모이즈된다.
- 페이지와 메타데이터는 동일 fetch 결과를 공유한다.
- 비-fetch 요청 단위 메모이즈는 `cache()`를 사용한다.
- 지속 캐시는 → [data](data.md)의 캐시 규칙을 따른다.
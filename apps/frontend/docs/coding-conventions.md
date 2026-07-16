# Coding Conventions

## 언제

- 타입·파일·심볼 이름을 짓거나, 새 값을 어떤 타입으로 선언할지 정할 때(배치·패키지 이름은 → [architecture](architecture.md)).
- 컴포넌트를 쓸 때. 세그먼트 배치·서버/클라 판단은 → [architecture](architecture.md)·[rendering](rendering.md), 여기선 표기·명명·접근성만.
- 스타일을 입힐 때. 새 도메인 개념의 표준어를 정할 때.

## 규칙

- 포맷은 Prettier, 린트는 ESLint가 강제한다. 수동 스타일 논쟁을 하지 않는다 → [code-quality](code-quality.md)의 Prettier·ESLint.

### 네이밍

- 컴포넌트 파일·export는 PascalCase, 훅은 `use`-접두 camelCase, 그 외 모듈은 camelCase.
- 파일명은 역할을 드러낸다: 컴포넌트 `Button.tsx`, 스토리 `Button.stories.tsx`, 훅 `useCart.ts`, Server Action 파일 `actions.ts`, route handler `route.ts`, 스키마 `schema.ts`, 슬라이스 public API `index.client.ts`/`index.server.ts`.
- 라우트 파일(`page`·`layout`·`loading`·`error` 등)은 `export default`로 내보낸다 — 네임드 export는 라우터가 인식하지 못한다. feature 컴포넌트는 네임드 export를 쓴다.
- 벤더 중립으로 짓는다. 백엔드·인프라 벤더명을 타입·심볼에 넣지 않는다(api-client 포트의 벤더 중립 명명은 → [data](data.md)의 api-client).
- 한 개념에는 하나의 표준어만 쓴다. 동의어를 만들지 않는다.
  - 코드·UI 카피·타입명·API 필드가 같은 개념을 다르게 부르면 드리프트가 쌓인다. 도메인 표준어는 그 도메인 슬라이스의 로컬 용어집에 둔다.

### 타입

- 백엔드 API 타입은 계약에서 코드젠한 Zod 스키마로부터 `z.infer`로 파생한다. TS 타입과 Zod를 각각 손으로 두지 않는다(단일 소스).
  - 스키마가 단일 원천이고 타입은 그 추론물이다. 이중 소스는 런타임 검증과 컴파일 타입이 조용히 어긋난다.
- `any`를 쓰지 않는다. 경계 입력은 `unknown`으로 받아 Zod로 좁힌다. 어느 경계가 무엇을 검증하는지는 → [data](data.md)의 응답 형상·api-client.
- TypeScript strict를 전제한다(설정은 → [code-quality](code-quality.md)의 TypeScript).

### 구조

- 매직보다 명시를 택한다. 과한 추상화·배럴 남용·동적 재export를 피한다.
  - 서버/클라 라인을 가로지르는 배럴 금지의 근거는 → [architecture](architecture.md)의 세그먼트와 서버/클라 구조 경계.

### 컴포넌트

- 컴포넌트의 세그먼트 배치는 → [architecture](architecture.md)의 세그먼트와 서버/클라 구조 경계.
- 재사용 디자인시스템 컴포넌트는 `packages/ui`로, feature 전용은 슬라이스 `ui`로 둔다. 도메인 로직을 `packages/ui`에 넣지 않는다. 계층 구분·승격 기준·prop API·완료 정의는 → [design-system](design-system.md)의 컴포넌트 계층·컴포넌트 API·완료 정의.
- 컴포넌트의 서버/클라 기본값과 `'use client'` leaf 판단은 → [rendering](rendering.md)의 렌더 경계.
- 접근성을 완료조건으로 만든다.
  - 시맨틱 HTML을 먼저 쓴다(`button`·`nav`·`main`·`ul`). `div` 클릭 핸들러로 버튼을 흉내내지 않는다.
  - 인터랙티브 요소는 키보드 조작·포커스 표시를 갖는다. 폼 입력은 연결된 `label`을 갖는다(placeholder를 라벨 대용으로 쓰지 않는다).
  - 정적 검사는 실제 접근성의 일부만 잡는다(강제 범위는 → [code-quality](code-quality.md)의 ESLint). 나머지는 이 판단이 소유한다.
- 이미지·폰트를 최적화한다: `next/image`(명시적 dimensions·LCP 이미지 `priority`·컨테이너 채움 `fill`)로 CLS를, `next/font`로 폰트 레이아웃 시프트를 없앤다.

### 스타일링

- Tailwind CSS v4 유틸리티 + 디자인 토큰을 쓴다. 원시 hex·px 대신 토큰을 쓴다(원시값 강제는 → [code-quality](code-quality.md)의 ESLint).
  - CSS-in-JS(styled-components·vanilla-extract)를 기각한 이유: 런타임 비용 또는 파편화된 클래스 관리. 유틸리티 + 토큰이 일관성과 제거 용이성을 준다.
- 토큰의 체계·명명·테마 분기·소유는 → [design-system](design-system.md)의 토큰.
- 조건부 클래스는 명시적 유틸(`clsx`/`cn` 등)로 합성한다.

### 주석

- 구현 주석은 비자명한 "왜"만 적는다. 코드가 이미 말하는 What을 되풀이하지 않는다.
- 결정 내러티브를 주석에 넣지 않는다 — 무엇을 왜 채택·폐기했는지는 문서가 소유한다.
- 주석 처리된 코드를 남기지 않는다(git 이력이 보존한다).
- TODO에는 이슈 번호를 단다(`// TODO(#123): …`).

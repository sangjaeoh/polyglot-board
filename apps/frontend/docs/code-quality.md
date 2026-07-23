# Code Quality

## 언제

- 포맷·타입·린트·경계·워크벤치 게이트의 강제 범위를 확인할 때.
- 게이트 도구 버전을 정하거나 올릴 때.
- 새 앱·패키지에 품질 게이트를 연결할 때.

## 규칙

### 품질 게이트 소유

- 강제 장치는 중앙 설정이 소유한다.
  - ESLint: `packages/eslint-config`
  - TypeScript: `packages/tsconfig`
  - 경계 검사: 루트 dependency-cruiser 설정
  - 실행 그래프: `turbo.json`
- 앱·패키지는 공유 설정을 상속한다.
- 강제 로직을 앱마다 복제하지 않는다.
- 로컬 명령과 CI 명령은 동일하다.
- `turbo`가 워크스페이스 그래프 실행과 캐싱을 담당한다.
- 경계 강제(워크스페이스 방향·FSD 방향·서버/클라이언트 누수)의 불변식 목록은 → [architecture](architecture.md)의 빌드가 강제하는 불변식이 소유한다.

### Prettier

- Prettier를 포맷 게이트로 사용한다.
- 포맷 위반은 자동 교정한다.
- 수동 스타일 문서를 두지 않는다.
- `eslint-config-prettier`로 ESLint 포맷 규칙 충돌을 제거한다.

### ESLint

- ESLint가 린트·React·접근성·코드 수준 경계를 강제한다.
- 적용 플러그인은 `@next/eslint-plugin-next`·`eslint-plugin-react-hooks`(v7)·`eslint-plugin-jsx-a11y`·`eslint-plugin-boundaries`다.
- 원시 색·치수 값 등 토큰 우회는 ESLint에서 차단한다.
- Biome는 사용하지 않는다.
  - `@next/eslint-plugin-next`의 App Router·Core Web Vitals 규칙 대응이 없다.
  - `eslint-plugin-react-hooks` v7의 React Compiler 계열 린트(`recommended-latest`) 대응이 없다.
  - React Compiler 활성 여부와 관계없이 해당 린트는 유지한다.
- ESLint와 Prettier 병행 비용은 flat config와 `eslint-config-prettier`로 상쇄한다.

### TypeScript

- 타입 게이트는 turbo 태스크로 실행하는 패키지별 `tsc --noEmit`이다.
- `strict`를 활성화한다.
- 타입 작성 규칙은 → [coding-conventions](coding-conventions.md)이 소유한다.

### dependency-cruiser

- dependency-cruiser로 워크스페이스 의존 그래프를 검사한다.
- 검사 대상은 워크스페이스 방향·순환·고아 모듈이다.
- 불변식 정의는 → [architecture](architecture.md)의 빌드가 강제하는 불변식이 소유한다.

### Storybook

- 디자인시스템 워크벤치는 Storybook(CSF3)을 사용한다.
- 스토리는 대상 컴포넌트 옆에 둔다.
- 파일 명명 규칙은 → [coding-conventions](coding-conventions.md)이 소유한다.
- 워크벤치 규칙은 → [design-system](design-system.md)이 소유한다.
- Ladle 등 경량 대안은 사용하지 않는다.
  - 접근성 애드온·테스트 통합 생태계가 부족해 자동 검사 규칙을 이행할 수 없다.
- 선택 기준은 렌더링 성능이 아니라 검사 체인 구성 가능 여부다.
- Storybook 설정은 디자인시스템 패키지가 소유한다.
- 앱에 Storybook 설정을 두지 않는다.
  - 앱에 두면 디자인시스템 검증이 앱 구조에 종속된다.
- 게이트에 전 스토리 렌더 스모크(`@storybook/addon-vitest`)와 접근성 검사(addon-a11y·axe)를 포함한다.
- 비주얼 회귀 검사는 도입 시점에 이 문서에서 고정한다.
- 비주얼 회귀 검사는 선제 도입하지 않는다.

### 게이트 예외

- 타입·린트·경계 게이트 위반은 설계 변경으로 해결한다.
- `eslint-disable`·`@ts-ignore`·`@ts-expect-error`로 게이트를 무력화하지 않는다.
- 예외:
  - 억제가 불가피하면 최소 범위로 제한한다.
  - 비자명한 이유 주석을 작성한다.

### 새 의존성

- 새 npm 의존성 도입은 신중히 검토한다.
  - 프론트 의존성은 런타임 번들에 포함되어 공급망 리스크를 가진다.
- 단순 버전 상향은 예외다.

### 버전

- 정확한 버전은 각 프로젝트 `package.json`·`pnpm-lock.yaml`이 정본이다.
- 아래 표는 게이트 툴체인의 baseline만 고정한다.
- 런타임 프레임워크·라이브러리 버전은 → [README](../README.md)의 기술 스택.

| 항목 | baseline |
|---|---|
| Node.js | 24 (LTS) |
| TypeScript | strict, 최신 5.x |
| 패키지 매니저 | pnpm(workspaces) · Turborepo |
| 린트 | ESLint (`@next/eslint-plugin-next`·`react-hooks` v7·`jsx-a11y`·`boundaries`) |
| 포맷 | Prettier + `eslint-config-prettier` |
| 경계 | dependency-cruiser |
| 워크벤치 | Storybook(CSF3 · addon-vitest · addon-a11y) |
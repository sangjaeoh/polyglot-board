# Code Quality

## 언제

- 포맷·타입·린트·경계 게이트가 무엇을 강제하는지 확인할 때.
- 게이트 도구 버전을 정하거나 올릴 때. 런타임 프레임워크·라이브러리 버전은 → [README](../README.md)의 기술 스택.
- 새 앱·패키지에 품질 게이트를 배선할 때.

## 규칙

- 강제 기계 배치: ESLint flat config는 앱 인라인(`apps/web/eslint.config.mjs`), TS 베이스는 프론트 루트 `tsconfig.base.json`(전 워크스페이스가 상속), dependency-cruiser는 프론트 루트 `.dependency-cruiser.cjs`, 태스크 그래프는 모노레포 루트 `turbo.json`이다. 공유 `packages/eslint-config`·`packages/tsconfig` 패키지는 미구현(향후)이다.
  - 강제 로직을 앱마다 복제하면 드리프트한다 — 둘째 앱을 추가할 때 공유 패키지로 승격한다. 검증은 루트 `verify` 파이프라인 한 명령으로 실행한다(`turbo`가 워크스페이스 그래프에 팬아웃·캐싱).
- 경계 강제(워크스페이스 방향·FSD 방향·서버/클라 누수)의 불변식 목록은 → [architecture](architecture.md)의 빌드가 강제하는 불변식이 소유한다. 이 문서는 그 도구의 설정만 다루고 불변식을 재서술하지 않는다.

### Prettier

- Prettier가 포맷 게이트다. 어긋난 포맷은 자동 교정한다.
- 수동 스타일 문서를 두지 않는다. `eslint-config-prettier`로 포맷 관련 ESLint 규칙을 꺼 충돌을 배선으로 없앤다.
  - 사람이 지키는 스타일은 리뷰 소음이 되고 결국 어긋난다. 포맷·스타일 논쟁을 자동 교정으로 없앤다.

### ESLint

- ESLint가 린트·React·접근성·경계를 강제한다: `@next/eslint-plugin-next`·`eslint-plugin-react-hooks`(v6)·`eslint-plugin-jsx-a11y`·`eslint-plugin-boundaries`. 원시 색·치수 값(토큰 우회)도 여기서 막는다(규칙은 → [coding-conventions](coding-conventions.md)의 스타일링).
  - Biome를 기각한 이유: `@next/eslint-plugin-next`(App Router·Core Web Vitals 규칙)에 대응물이 없고, `eslint-plugin-react-hooks` v6에 번들된 React Compiler 계열 린트(`recommended-latest`로 opt-in)가 Biome엔 없다 — 이 둘이 load-bearing이다. React Compiler가 기본 off라도(→ [architecture](architecture.md)의 설정 불변식) 이 린트는 유효하다.
  - 도구가 둘인 대가는 `eslint-config-prettier`로 충돌을 끄고 flat config로 배선해 상쇄한다.

### TypeScript

- `tsc --noEmit`(project references)가 타입 게이트다. strict를 켠다(타이핑 관용구는 → [coding-conventions](coding-conventions.md)의 타입).

### dependency-cruiser

- dependency-cruiser가 워크스페이스 의존 그래프를 검사한다. 무엇을 막는지(방향·순환·고아·드롭 레이어)는 → [architecture](architecture.md)의 빌드가 강제하는 불변식. 이 문서는 설정 위치·상속만 소유한다.

### 억제

- 게이트(타입·린트·경계) 위반은 억제가 아니라 설계 변경으로 없앤다. `eslint-disable`·`@ts-ignore`·`@ts-expect-error`로 게이트를 침묵시키지 않는다.
  - 억제가 불가피하면 최소 스코프로 좁히고 비자명한 이유 주석을 단다. 억제로 도망치면 게이트가 무력화된다.

### 새 의존성

- 새 npm 의존성 도입은 신중히 한다 — 프론트 의존성은 런타임 번들에 실려 사용자에게 배포되므로 공급망 리스크가 크다(단순 버전 상향 제외).

### 버전

- 정확한 버전 정본은 각 프로젝트 `package.json`·`pnpm-lock.yaml`이다. 아래 표는 게이트 툴체인의 baseline만 고정한다(런타임 프레임워크·라이브러리 버전은 → [README](../README.md)의 기술 스택).

  | 항목                | baseline                                                                 |
  | ------------------- | ------------------------------------------------------------------------ |
  | Node.js             | 24 (LTS)                                                                  |
  | TypeScript          | strict, 최신 5.x                                                         |
  | 패키지 매니저       | pnpm (workspaces) · Turborepo                                            |
  | 린트                | ESLint (`@next/eslint-plugin-next`·`react-hooks` v6·`jsx-a11y`·`boundaries`) |
  | 포맷                | Prettier (+ `eslint-config-prettier`)                                    |
  | 경계                | dependency-cruiser                                                       |

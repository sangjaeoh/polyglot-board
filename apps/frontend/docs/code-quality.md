# Code Quality

## 언제

- 포맷·타입·린트·경계·워크벤치 게이트가 무엇을 강제하는지 확인할 때.
- 게이트 도구 버전을 정하거나 올릴 때. 런타임 프레임워크·라이브러리 버전은 → [README](../README.md)의 기술 스택.
- 새 앱·패키지에 품질 게이트를 배선할 때.

## 규칙

- 강제 기계 배치: ESLint flat config는 린트 대상 워크스페이스마다 인라인이다(앱 `apps/web/eslint.config.mjs`, 디자인시스템 패키지 `packages/ui/eslint.config.mjs`). 워크스페이스가 공유하는 규칙(원시 색·치수 값 차단 등)은 프론트 루트 `eslint-rules/`가 소유하고 각 인라인 config가 소비한다. TS 베이스는 프론트 루트 `tsconfig.base.json`(전 워크스페이스가 상속), dependency-cruiser는 프론트 루트 `.dependency-cruiser.cjs`, 태스크 그래프는 모노레포 루트 `turbo.json`이다.
  - 강제 로직을 앱마다 복제하면 드리프트한다 — 둘째 앱을 추가할 때 공유 `packages/eslint-config`·`packages/tsconfig` 패키지로 승격한다. 검증은 루트 `verify` 파이프라인 한 명령으로 실행한다(`turbo`가 워크스페이스 그래프에 팬아웃·캐싱).
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

### Storybook

- 디자인시스템 워크벤치는 Storybook(CSF3)이다. 스토리는 대상 컴포넌트 옆에 둔다(파일 명명은 → [coding-conventions](coding-conventions.md)의 네이밍). 워크벤치가 지키는 규칙(계약·커버리지·강제 범위)은 → [design-system](design-system.md)의 워크벤치.
  - Ladle 등 경량 대안을 기각한 이유: 접근성 애드온·test-runner 생태계가 없거나 빈약해 스토리 대상 자동 검사 규칙을 이행할 수 없다. load-bearing한 것은 렌더러가 아니라 검사 체인이다.
- Storybook 설정은 디자인시스템 패키지가 소유한다. 앱에 두지 않는다.
  - 특정 앱에 살면 둘째 앱부터 소유가 모호해지고 디자인시스템 검증이 앱 설정에 묶인다. 워크벤치가 렌더하는 토큰·스타일은 디자인시스템 패키지 소유가 전제다(→ [design-system](design-system.md)의 토큰).
- 빌더는 react-vite다.
  - next 빌더를 기각한 이유: 디자인시스템 패키지는 Next 밖 순수 React라 next 빌더가 워크벤치를 앱에 결합시킨다. 워크벤치는 앱 없이 렌더해야 한다.
- Tailwind는 워크벤치 자체 Vite 빌드가 처리한다(앱 postcss와 분리). 프리뷰는 디자인시스템 패키지의 토큰 스타일시트를 직접 import해 앱 없이 렌더한다.
- hover·focus-visible·active 상태는 pseudo-states 애드온으로 상태 매트릭스에 렌더한다.
  - 정적 스토리는 의사 클래스 상태를 캡처하지 못한다. 애드온이 상태를 강제해 매트릭스가 계약으로 고정된다.
- 게이트: 전 스토리 렌더 스모크(test-runner)와 접근성 검사(addon-a11y·axe)를 루트 `verify` 파이프라인에 포함한다.
  - 스토리가 있어도 verify 밖이면 깨진 채 방치된다. 게이트 합류가 강제의 실체다.
- 시각 회귀 검사 도구는 도입 시점에 이 문서가 고정한다. 선제 도입하지 않는다(검사 대상 규칙은 → [design-system](design-system.md)의 워크벤치).

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
  | 워크벤치            | Storybook (react-vite · CSF3 · test-runner · addon-a11y · addon-pseudo-states) |

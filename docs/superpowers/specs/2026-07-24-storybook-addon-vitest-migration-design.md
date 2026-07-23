# Storybook 워크벤치 게이트: test-runner → addon-vitest 마이그레이션

## 배경

`apps/frontend`의 가이드(nextjs-agent-guide 최신본)를 `docs/*.md`에 통째로 덮어썼다. 그 결과 `code-quality.md`가 "게이트에 전 스토리 렌더 스모크(`@storybook/addon-vitest`)와 접근성 검사(addon-a11y·axe)를 포함한다"로 바뀌었는데, `packages/ui`는 아직 `@storybook/test-runner` + `axe-playwright` 기반이다. 가이드와 실제 코드가 어긋난 3건 중 나머지 2건(react-hooks v7, eslint-boundaries `app` 레이어 허용범위 확장)은 단순 설정값 변경이라 별도 처리하고, 이 스펙은 Storybook 워크벤치 게이트 마이그레이션만 다룬다.

## 현재 구조 (packages/ui)

- `.storybook/main.ts` — `@storybook/react-vite` 빌더, addon: `@storybook/addon-a11y`, `storybook-addon-pseudo-states`.
- `.storybook/test-runner.ts` — `@storybook/test-runner` 훅. `preVisit`에서 `axe-playwright`의 `injectAxe`. `postVisit`에서 라이트·다크 두 테마 각각 `page.emulateMedia({colorScheme})`로 강제한 뒤 `checkA11y` — 스토리 렌더 스모크 + 듀얼 테마 a11y를 한 번의 방문에서 수행.
- `.storybook/preview.tsx` — 워크벤치 테마 툴바는 `tokens.css` 원시값을 인라인 스타일로 얹는 트릭(프리뷰 전용). 주석으로 "게이트는 이 토글이 아니라 실제 `prefers-color-scheme` 미디어 에뮬레이션으로 검증 — 두 경로가 교차검증"이라고 명시. **이 이원화 설계는 유지 대상.**
- `packages/ui/scripts/test-stories.mjs` — Storybook 정적 빌드 → Node 내장 http 서버로 서빙 → `test-storybook` CLI 실행. 이유: "게이트는 오프라인·비대화형이다 — 정적 빌드가 dev 서버 온디맨드 컴파일보다 결정적이라 게이트에 적합하다."
- `package.json` scripts: `test-stories` → `node ./scripts/test-stories.mjs`, `verify` → `test-stories`.
- devDependencies 관련: `@storybook/test-runner: 0.24.4`, `axe-playwright: 2.2.2`, `playwright: 1.61.1`, `storybook: 10.5.0`, `@storybook/addon-a11y: 10.5.0`.
- vitest는 레포 타 패키지(`packages/api-client`, `apps/web`)에서 이미 사용 중 — 툴체인 일관성 측면에서 addon-vitest 전환이 자연스럽다.

## 조사한 제약 (Context7 공식 문서 확인)

- Vitest 브라우저 모드는 테스트/셋업 코드에 Playwright의 raw `page`를 넘기지 않는다. `page.emulateMedia`처럼 Playwright 전용 API가 필요하면 **커스텀 Browser Command**(서버사이드 함수, `vitest.config.ts`의 `test.browser.commands`에 등록)로만 접근 가능하다 — 커맨드 구현부에서는 `BrowserCommandContext.page`(playwright provider가 주입하는 raw Page)를 쓰고, 테스트 쪽에서는 `commands.xxx(...)`로 호출한다.
- `@storybook/addon-vitest`는 `storybookVitest()` Vite 플러그인(`@storybook/addon-vitest/vite`)을 `vitest.config.ts`에 꽂고 `test.browser.enabled = true`(playwright provider, chromium, headless)로 설정하는 방식이다. Storybook의 portable-stories 메커니즘으로 각 스토리를 Vitest 테스트로 변환해 Vite 그래프에 직접 대고 돈다 — 별도 정적 빌드·서버가 필요 없다.
- 접근성 검사는 `.storybook/vitest.setup.ts`에서 `setProjectAnnotations`에 `@storybook/addon-a11y/preview`의 annotation을 등록하는 방식으로 배선한다. CI(비대화형 `vitest run`)에서는 `parameters.a11y.test = 'error'`가 설정된 스토리에 대해 a11y 검사가 자동 실행된다. `axe-playwright` 패키지 없이 addon-a11y 내장 axe-core로 처리된다.
- `package.json` 스크립트 예시: `"test-storybook": "vitest --project=storybook"`.

## 설계

### 1. `packages/ui/vitest.config.ts` (신규)

- `storybookVitest()` 플러그인 등록.
- `test.browser`: `enabled: true`, `name: 'chromium'`, `headless: true`, `provider: 'playwright'`.
- `test.browser.commands`에 `setColorScheme` 커스텀 커맨드 추가 — 서버사이드에서 `context.page.emulateMedia({ colorScheme })` 호출. 기존 `postVisit`의 `page.emulateMedia` 호출을 그대로 이 커맨드로 이식.
- `test.setupFiles`에 `.storybook/vitest.setup.ts` 등록.
- Storybook 전용 실행과 다른 vitest 실행(있다면)을 구분해야 하면 `test.projects`(Vitest workspace) 또는 `name: 'storybook'` 프로젝트 태그로 격리 — `vitest --project=storybook`이 동작하도록.

### 2. `packages/ui/.storybook/vitest.setup.ts` (신규)

- `setProjectAnnotations([previewAnnotations, a11yAddonAnnotations])`로 `preview.tsx`(기존 설정 유지) + `@storybook/addon-a11y/preview`를 등록하고 `beforeAll(annotations.beforeAll)` 실행.
- 전역 `afterEach` 훅에서 스토리 렌더 후:
  1. `commands.setColorScheme('light')` 호출 → a11y 검사(기존 `checkA11y` 동등 기능을 addon-a11y 경로로) 수행.
  2. `commands.setColorScheme('dark')` 호출 → 동일 a11y 검사 재수행.
  - `preview.tsx`의 `a11y.config.rules`(예: 고립 스토리 `region` off) 반영 — 기존 `storyContext.parameters?.a11y?.config?.rules` 처리를 addon-a11y 설정 경로로 이식.
- 렌더 자체가 실패(throw)하면 테스트가 실패 — 기존 test-runner의 "렌더 스모크"와 동등.

### 3. `packages/ui/.storybook/main.ts`

- `addons` 배열에 `@storybook/addon-vitest` 추가. `@storybook/addon-a11y`, `storybook-addon-pseudo-states`는 유지.

### 4. `packages/ui/.storybook/test-runner.ts` 삭제

- 로직은 2번 setup 파일로 완전 이전 완료 후 삭제.

### 5. `packages/ui/scripts/test-stories.mjs` 삭제, `package.json` 스크립트 변경

- 정적 빌드→서빙→CLI 오케스트레이션이 필요 없어짐(addon-vitest가 Vite 그래프에 직접 붙음).
- `test-stories` 스크립트를 `vitest run --project=storybook`(또는 workspace 미사용 시 단순 `vitest run`)로 교체.
- 오프라인·비대화형 요구 유지: 실행 env에 `STORYBOOK_DISABLE_TELEMETRY=1`, `STORYBOOK_TELEMETRY_DISABLED=1` 유지(스크립트 레벨 또는 vitest.config 레벨).
- `verify` 스크립트는 그대로 `test-stories`를 호출.

### 6. 의존성 변경 (`packages/ui/package.json`)

- 제거: `@storybook/test-runner`, `axe-playwright`.
- 추가: `@storybook/addon-vitest`, `vitest`, `@vitest/browser`(브라우저 모드 런타임).
- 유지: `playwright`(Vitest playwright provider가 그대로 사용), `@storybook/addon-a11y`, `storybook`, `@storybook/react-vite`, `storybook-addon-pseudo-states`.
- 버전은 설치 시점 `storybook@10.5.0`과 호환되는 최신으로 pnpm이 해석한 값을 그대로 사용(가이드 code-quality.md: "정확한 버전은 각 프로젝트 package.json·pnpm-lock.yaml이 정본이다").

### 유지되는 것 (변경 범위 밖)

- `preview.tsx`의 툴바 vs 게이트 이원화 구조(주석에 명시된 의도적 설계) — 그대로.
- 라이트·다크 듀얼 테마 검사 대상·판정 기준 — 그대로(테마당 1회씩, 총 2회 a11y 검사).
- `region` 규칙 off 등 스토리별 a11y 설정 오버라이드 — 그대로.

## 검증

- `pnpm --filter @board/ui run test-stories` (또는 교체된 스크립트명)가 종료 코드 0으로 끝난다.
- 의도적으로 접근성 위반 스토리(임시)를 추가해 실행 시 실패하는지 확인 후 되돌린다 — a11y 검사가 실제로 작동하는지 검증.
- `vitest.config.ts`의 `setColorScheme` 커맨드가 라이트/다크 각각 실제로 `prefers-color-scheme` 미디어를 바꾸는지, 다크 전용 토큰 위반 스토리(임시)로 검증 후 되돌린다.
- `pnpm --filter @board/ui run build-storybook`가 여전히 정상 동작(빌드 자체는 이번 변경과 무관하지만 addon 교체가 빌드를 깨지 않는지 확인).
- `git grep -n "test-runner\|axe-playwright"` 결과가 `packages/ui` 범위에서 0건.

## 범위 밖

- react-hooks v7 업그레이드, eslint-boundaries `app` 레이어 확장 — 별도 처리(이 스펙 대상 아님).
- `preview.tsx`의 테마 툴바 UX, `tokens.css` 파싱 로직 변경 — 대상 아님.
- Chromatic 등 시각적 회귀 검사 도입 — 가이드(code-quality.md)가 "비주얼 회귀 검사는 도입 시점에 문서에서 고정, 선제 도입하지 않는다"로 명시 — 대상 아님.

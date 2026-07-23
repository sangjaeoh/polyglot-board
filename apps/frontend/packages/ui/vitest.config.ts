import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { defineConfig } from 'vitest/config';
import type { BrowserCommand } from 'vitest/node';

import { storybookTest } from '@storybook/addon-vitest/vitest-plugin';

import { playwright } from '@vitest/browser-playwright';

import { checkA11y, configureAxe, injectAxe } from 'axe-playwright';

const dirname =
  typeof __dirname !== 'undefined' ? __dirname : path.dirname(fileURLToPath(import.meta.url));

// a11y 게이트(test-runner.ts 대체)가 라이트·다크 양 테마를 검증하려면 실제 배포 경로인
// prefers-color-scheme 미디어 에뮬레이션이 필요하다. vitest/browser의 page는 Vitest 전용
// BrowserPage 래퍼라 Playwright의 raw page.emulateMedia에 닿지 못하므로, provider가 커맨드
// 컨텍스트에만 노출하는 raw page를 서버 사이드 커맨드로 감싼다.
type ColorScheme = 'light' | 'dark' | 'no-preference';

// setColorScheme(page.emulateMedia)가 반환한 뒤 곧바로 axe를 돌리면 색상 대비가 오탐으로 깨진다.
// 원인은 커스텀 프로퍼티 재계산 지연이 아니다(실측: matchMedia 일치 + :root --primary 계산값
// 일치까지 폴링해도 15/21 오탐 재현 — 둘 다 이미 새 스킴 값이었다). 실제 원인은 Button 등이 쓰는
// `transition-[background-color,box-shadow,transform] duration-fast`(150ms) CSS 트랜지션이다:
// prefers-color-scheme 전환으로 background-color의 목표값이 바뀌면 브라우저가 이전 값에서 새
// 값으로 150ms에 걸쳐 실제로 보간 애니메이션하고, getComputedStyle은 그 순간의 중간값을 그대로
// 반환한다(실측: 전환 직후 30ms 간격으로 8회 샘플링한 배경색이 21,27,35 → 39,44,52 → 114,119,125
// → … → 232,236,241로 점진적으로 변함 — 재계산 실패가 아니라 진행 중인 애니메이션이었다). 그래서
// matchMedia로 미디어 상태 전환을 확인한 뒤, 이 스킴 전환이 실제로 일으킨 CSS 트랜지션들이 끝날
// 때까지 기다린다. document.getAnimations()는 CSS 트랜지션과 무관한 향후의 무한 반복
// @keyframes 애니메이션(예: 로딩 스피너)도 함께 잡을 수 있어 CSSTransition 인스턴스만 걸러
// 기다린다 — 그런 애니메이션은 절대 끝나지 않아 Promise.all을 무한 대기시키기 때문이다.
const setColorScheme: BrowserCommand<[scheme: ColorScheme]> = async ({ page, frame }, scheme) => {
  await page.emulateMedia({ colorScheme: scheme });
  const expectedDark = scheme === 'dark';
  const testFrame = await frame();
  // 타임아웃은 안전망이다 — 스킴 적용이 실제로 실패하면 무한 대기 대신 몇 초 안에 명확한
  // 에러로 빨리 실패한다.
  await testFrame.waitForFunction(
    (expected) => window.matchMedia('(prefers-color-scheme: dark)').matches === expected,
    expectedDark,
    { timeout: 5000 },
  );
  await testFrame.evaluate(() => {
    const transitions = document.getAnimations().filter((a) => a instanceof CSSTransition);
    return Promise.race([
      Promise.all(transitions.map((t) => t.finished.catch(() => undefined))),
      new Promise((resolve) => setTimeout(resolve, 5000)),
    ]);
  });
};

// vitest/browser의 page에는 evaluate가 없어 matchMedia 결과를 테스트 파일에서 직접 읽을 수
// 없다 — setColorScheme 회귀 테스트 전용으로, 커맨드 컨텍스트의 raw Playwright page.evaluate로
// 브라우저 안에서 matchMedia를 읽어 반환한다.
const matchesDarkColorScheme: BrowserCommand<[], boolean> = async ({ page }) => {
  return page.evaluate(() => window.matchMedia('(prefers-color-scheme: dark)').matches);
};

// a11y 게이트(test-runner.ts의 injectAxe/checkA11y 대체)도 raw page가 필요하다: addon-a11y가
// 공개하는 건 프로젝트 어노테이션용 afterEach 훅뿐이라 테마별로 두 번 부를 수 있는 콜러블 run
// 함수가 없다(패키지가 export하는 건 afterEach/decorators/initialGlobals/parameters뿐 — 실제
// axe.run을 감싼 run()은 비공개). 그래서 test-runner.ts와 동일한 axe-playwright를 raw
// Playwright 프레임에 직접 구동한다. page가 아니라 commands 컨텍스트의 frame()을 쓴다 —
// vitest/browser-playwright에서 page는 오케스트레이터를 띄우는 최상위 페이지이고, 실제 스토리는
// 그 안의 "vitest-iframe" 프레임(Storybook의 iframe.html)에 렌더링된다(실측: page 기준으로
// axe를 돌리면 스토리가 아니라 오케스트레이터 셸을 스캔해 frame-title/landmark-one-main 같은
// 무관한 위반이 매번 뜬다). axe-playwright의 타입은 Playwright Page를 받지만 실제로는
// page.evaluate만 쓰므로, 같은 evaluate API를 가진 Frame을 그대로 넘겨도 런타임엔 동일하게
// 동작한다. 컨텍스트는 'body'다 — 이 프레임은 iframe.html 템플릿(sb-preparing-story 등 숨은
// 상태 위젯 포함)을 그대로 쓰지만 실제 스토리는 #storybook-root가 아니라 그 옆에 이름 없는
// 컨테이너 div로 붙는다(portable stories runStory) — 'body'로 스캔해도 숨은 상태 위젯은
// 비가시라 axe가 잡지 않는다(실측).
type AxeRules = NonNullable<Parameters<typeof configureAxe>[1]>['rules'];
type AxeRunOptions = NonNullable<Parameters<typeof checkA11y>[2]>['axeOptions'];
type AxePage = Parameters<typeof injectAxe>[0];

const runAxeCheck: BrowserCommand<[options?: { rules?: AxeRules; axeOptions?: AxeRunOptions }]> = async (
  { frame },
  { rules, axeOptions } = {},
) => {
  const testFrame = (await frame()) as unknown as AxePage;
  await injectAxe(testFrame);
  if (rules) await configureAxe(testFrame, { rules });
  await checkA11y(testFrame, 'body', { detailedReport: false, axeOptions });
};

declare module 'vitest/browser' {
  interface BrowserCommands {
    setColorScheme: (scheme: ColorScheme) => Promise<void>;
    matchesDarkColorScheme: () => Promise<boolean>;
    runAxeCheck: (options?: { rules?: AxeRules; axeOptions?: AxeRunOptions }) => Promise<void>;
  }
}

// storybookTest 플러그인은 project.test.include를 스토리 글롭으로 강제 대체한다(사용자가
// 지정해도 경고와 함께 무시된다) — 커맨드 자체의 회귀 테스트(vitest.*.test.ts)는 그 project에
// 실릴 수 없어, 같은 browser 설정을 공유하는 별도 project로 분리한다.
const browser = () => ({
  enabled: true,
  headless: true,
  provider: playwright({}),
  instances: [{ browser: 'chromium' as const }],
  commands: { setColorScheme, matchesDarkColorScheme, runAxeCheck },
});

// More info at: https://storybook.js.org/docs/next/writing-tests/integrations/vitest-addon
export default defineConfig({
  test: {
    projects: [
      {
        extends: true,
        plugins: [
          // The plugin will run tests for the stories defined in your Storybook config
          // See options at: https://storybook.js.org/docs/next/writing-tests/integrations/vitest-addon#storybooktest
          storybookTest({ configDir: path.join(dirname, '.storybook') }),
        ],
        test: {
          name: 'storybook',
          browser: browser(),
          // storybookTest의 config() 훅은 이 값이 배열이면 통째로 버리고 자체 internal
          // setupFiles로 덮어쓴다(문자열일 때만 보존하는 알려진 동작) — 그래서 배열이 아니라
          // 단일 문자열로 넘긴다.
          setupFiles: path.join(dirname, '.storybook/vitest.setup.ts'),
        },
      },
      {
        extends: true,
        test: {
          name: 'browser-commands',
          include: ['vitest.*.test.ts'],
          browser: browser(),
        },
      },
    ],
  },
});

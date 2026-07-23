import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { defineConfig } from 'vitest/config';
import type { BrowserCommand } from 'vitest/node';

import { storybookTest } from '@storybook/addon-vitest/vitest-plugin';

import { playwright } from '@vitest/browser-playwright';

const dirname =
  typeof __dirname !== 'undefined' ? __dirname : path.dirname(fileURLToPath(import.meta.url));

// a11y 게이트(test-runner.ts 대체)가 라이트·다크 양 테마를 검증하려면 실제 배포 경로인
// prefers-color-scheme 미디어 에뮬레이션이 필요하다. vitest/browser의 page는 Vitest 전용
// BrowserPage 래퍼라 Playwright의 raw page.emulateMedia에 닿지 못하므로, provider가 커맨드
// 컨텍스트에만 노출하는 raw page를 서버 사이드 커맨드로 감싼다.
type ColorScheme = 'light' | 'dark' | 'no-preference';

const setColorScheme: BrowserCommand<[scheme: ColorScheme]> = async ({ page }, scheme) => {
  await page.emulateMedia({ colorScheme: scheme });
};

// vitest/browser의 page에는 evaluate가 없어 matchMedia 결과를 테스트 파일에서 직접 읽을 수
// 없다 — setColorScheme 회귀 테스트 전용으로, 커맨드 컨텍스트의 raw Playwright page.evaluate로
// 브라우저 안에서 matchMedia를 읽어 반환한다.
const matchesDarkColorScheme: BrowserCommand<[], boolean> = async ({ page }) => {
  return page.evaluate(() => window.matchMedia('(prefers-color-scheme: dark)').matches);
};

declare module 'vitest/browser' {
  interface BrowserCommands {
    setColorScheme: (scheme: ColorScheme) => Promise<void>;
    matchesDarkColorScheme: () => Promise<boolean>;
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
  commands: { setColorScheme, matchesDarkColorScheme },
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

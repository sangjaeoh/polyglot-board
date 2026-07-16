import type { TestRunnerConfig } from '@storybook/test-runner';
import { getStoryContext, waitForPageReady } from '@storybook/test-runner';
import { injectAxe, configureAxe, checkA11y } from 'axe-playwright';

/*
 * 게이트(code-quality.md Storybook): 전 스토리 렌더 스모크 + 접근성(axe).
 *
 * 렌더 스모크는 test-runner 기본 방문이 담당한다 — 렌더나 play가 던지면 실패한다.
 * a11y는 postVisit에서 axe로 검사하되, 라이트·다크 양 테마에서 각각 돌린다(design-system.md 완료
 * 정의: 양 테마 확인). 다크는 shipped 토큰의 실제 경로인 prefers-color-scheme를 Playwright
 * 미디어 에뮬레이션으로 강제해 검증한다 — 프리뷰 툴바(워크벤치 전용 오버라이드)가 아니다.
 */
const SCHEMES = ['light', 'dark'] as const;

const config: TestRunnerConfig = {
  async preVisit(page) {
    await injectAxe(page);
  },
  async postVisit(page, context) {
    const storyContext = await getStoryContext(page, context);
    if (storyContext.parameters?.a11y?.disable) return;

    // 스토리·프리뷰가 선언한 axe 규칙(예: 고립 스토리의 'region' off)을 반영한다.
    const rules = storyContext.parameters?.a11y?.config?.rules;
    if (rules) await configureAxe(page, { rules });

    for (const colorScheme of SCHEMES) {
      await page.emulateMedia({ colorScheme });
      await waitForPageReady(page);
      await checkA11y(page, '#storybook-root', {
        detailedReport: false,
        axeOptions: storyContext.parameters?.a11y?.options,
      });
    }
    await page.emulateMedia({ colorScheme: 'no-preference' });
  },
};

export default config;

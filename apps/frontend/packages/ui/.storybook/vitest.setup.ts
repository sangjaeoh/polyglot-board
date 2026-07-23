import { afterEach } from 'vitest';
import { commands } from 'vitest/browser';

/*
 * 프로젝트 어노테이션(preview.tsx + main.ts의 addons, 즉 addon-a11y/preview 포함)은 이 파일에서
 * 수동으로 등록하지 않는다 — Storybook 10.3+의 @storybook/addon-vitest가 자동으로 주입한다.
 * 주의: 이 자동 주입은 addon-vitest가 이 setupFiles 파일의 소스 텍스트를 읽어, 프로젝트
 * 어노테이션을 등록하는 그 함수의 이름(패키지 "storybook/preview-api"가 내보내는 것과 같은
 * 이름)이 문자열로라도 들어있으면 "사용자가 직접 등록했다"고 오인해 꺼지는 단순 substring
 * 검사로 판단한다(실측) — 그러니 이 설명 주석에서조차 그 식별자를 문자 그대로 적으면 안 된다.
 * 자동 주입이 꺼지면 렌더 함수가 배선되지 않아 모든 스토리가 "No render function available"로
 * 깨진다(실측).
 */

declare module 'vitest' {
  interface TestContext {
    /**
     * @storybook/addon-vitest의 testStory(내부 전용, 공개 타입 없음)가 테스트 실행 중 세팅하는
     * composed story. 필요한 최소 형태(parameters)만 선언한다.
     */
    story?: { parameters?: Record<string, any> };
  }
}

/*
 * 게이트(code-quality.md Storybook): 전 스토리 렌더 스모크 + 접근성(axe).
 *
 * 렌더 스모크는 storybookTest가 생성한 테스트 본문(스토리 render+play)이 담당한다 — 던지면
 * 실패한다. a11y는 이 afterEach에서 axe로 검사하되, 라이트·다크 양 테마에서 각각 돌린다
 * (design-system.md 완료 정의: 양 테마 확인). 다크는 shipped 토큰의 실제 경로인
 * prefers-color-scheme를 Playwright 미디어 에뮬레이션으로 강제해 검증한다 — 프리뷰 툴바
 * (워크벤치 전용 오버라이드)가 아니다.
 *
 * addon-a11y가 공개하는 afterEach 훅은 테마를 모르고 스토리당 한 번만 자동으로 돈다(project
 * annotations로 합류해 composedStory.run() 안에서 실행됨) — 그래서 여기서는 그 훅에 기대지
 * 않고, vitest.config.ts의 runAxeCheck 커맨드(test-runner.ts와 동일한 axe-playwright 호출)를
 * 테마별로 직접 두 번 구동한다. story는 storybookTest가 만든 테스트 본문이 testStory 안에서
 * context.story에 세팅한 composed story — 같은 TestContext가 afterEach에도 전달되므로 여기서
 * 그대로 읽을 수 있다.
 */
const SCHEMES = ['light', 'dark'] as const;

afterEach(async (context) => {
  const story = context.story;
  if (!story) return; // 스토리 테스트가 아님(스킵되었거나 composed story가 없음)

  const a11yParams = story.parameters?.a11y;
  if (a11yParams?.disable) return;

  for (const scheme of SCHEMES) {
    await commands.setColorScheme(scheme);
    await commands.runAxeCheck({
      rules: a11yParams?.config?.rules,
      axeOptions: a11yParams?.options,
    });
  }
  await commands.setColorScheme('no-preference');
});

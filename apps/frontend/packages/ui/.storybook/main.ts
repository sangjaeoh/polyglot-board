import type { StorybookConfig } from '@storybook/react-vite';
import tailwindcss from '@tailwindcss/vite';

/*
 * 워크벤치 설정은 디자인시스템 패키지가 소유한다(code-quality.md Storybook). 앱에 두지 않는다.
 * 빌더는 @storybook/react-vite다 — packages/ui는 Next 밖 순수 React라 next 빌더는 앱 결합을 만든다.
 * Tailwind v4 처리는 앱 postcss가 소유하므로(apps/web postcss.config), 워크벤치는 자체 Vite
 * 빌드에 @tailwindcss/vite로 Tailwind를 배선한다(preview.css가 @import 'tailwindcss').
 */
const config: StorybookConfig = {
  framework: '@storybook/react-vite',
  // 스토리는 컴포넌트 옆에 co-locate한다(coding-conventions.md 네이밍).
  stories: ['../src/**/*.stories.@(ts|tsx)'],
  // a11y=axe 검사 대상, pseudo-states=hover·focus-visible·active를 매트릭스에 강제 렌더.
  addons: ['@storybook/addon-a11y', 'storybook-addon-pseudo-states'],
  viteFinal: (viteConfig) => {
    viteConfig.plugins = viteConfig.plugins ?? [];
    viteConfig.plugins.push(tailwindcss());
    return viteConfig;
  },
};

export default config;

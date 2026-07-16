import type { Preview } from '@storybook/react-vite';
import type { CSSProperties } from 'react';
import tokensRaw from '../src/tokens.css?raw';
import './preview.css';

/*
 * 양 테마 확인 수단(결정).
 *
 * shipped 토큰은 prefers-color-scheme 미디어쿼리로 라이트/다크를 전환한다(item 1 유지, 재작업 범위 밖).
 * 프리뷰 iframe 안에서 미디어쿼리 자체는 페이지가 강제할 수 없다 — color-scheme 속성은 네이티브
 * 컨트롤만 바꾸고 미디어 게이트 토큰은 뒤집지 못한다(그것이 prefers-color-scheme 에뮬레이션의 한계다).
 *
 * 그래서 툴바 토글은 tokens.css(단일 소스)에서 라이트/다크 팔레트 원시값을 빌드타임에 읽어(?raw),
 * 선택 테마의 원시값을 래퍼에 인라인 커스텀 프로퍼티로 얹는다 — 값을 워크벤치에 재작성하지 않는다
 * (design-system.md 변경과 폐기: 같은 역할 토큰 중복 금지). 이는 워크벤치 전용 경로다.
 *
 * 게이트(test-runner)는 이 토글이 아니라 실제 prefers-color-scheme 미디어 에뮬레이션으로 양 테마를
 * 자동 검증한다 — shipped 경로를 그대로 친다. 두 경로가 서로를 교차검증한다(code-quality.md 게이트).
 */
function paletteVars(block: string | undefined): CSSProperties | undefined {
  if (!block) return undefined;
  const vars: Record<string, string> = {};
  for (const decl of block.matchAll(/(--[\w-]+)\s*:\s*([^;]+);/g)) {
    vars[decl[1]] = decl[2].trim();
  }
  return Object.keys(vars).length ? (vars as CSSProperties) : undefined;
}

// 최상위 :root = 라이트 팔레트, @media (prefers-color-scheme: dark) 내 :root = 다크 팔레트.
const lightPalette = paletteVars(tokensRaw.match(/\n:root\s*\{([\s\S]*?)\}/)?.[1]);
const darkPalette = paletteVars(
  tokensRaw.match(/@media \(prefers-color-scheme: dark\)\s*\{\s*:root\s*\{([\s\S]*?)\}/)?.[1],
);

// 파싱은 tokens.css 포맷(최상위 :root, @media dark 내 :root)에 결합돼 있다. 포맷이 바뀌어
// 매칭이 깨지면 토글이 조용히 무력화되는 대신 크게 실패한다 — 프리뷰 로드 시 던지므로
// test-runner 스모크가 잡는다(게이트 커버리지 밖의 이 경로를 게이트로 끌어들인다).
if (!lightPalette || !darkPalette) {
  throw new Error(
    'tokens.css 팔레트 파싱 실패(:root 라이트 / @media dark) — 포맷 변경 시 preview.tsx 정규식을 갱신한다.',
  );
}

const preview: Preview = {
  parameters: {
    // 래퍼가 캔버스 전체를 칠하도록 프레이밍 여백을 끈다(래퍼가 자체 패딩·배경을 소유).
    layout: 'fullscreen',
    controls: { expanded: true },
    // 고립된 스토리엔 랜드마크가 없어 axe의 best-practice 'region'이 항상 위반이다 —
    // 컴포넌트 결함이 아니라 워크벤치 구조 산물이므로 끈다(게이트가 읽어 axe에 반영).
    a11y: {
      config: { rules: [{ id: 'region', enabled: false }] },
    },
  },
  initialGlobals: { theme: 'system' },
  globalTypes: {
    theme: {
      description: '테마 (dark는 tokens.css 원시값 오버라이드; 게이트는 실제 미디어 에뮬레이션으로 검증)',
      toolbar: {
        title: 'Theme',
        icon: 'contrast',
        items: [
          { value: 'system', title: 'System (prefers-color-scheme)' },
          { value: 'light', title: 'Light' },
          { value: 'dark', title: 'Dark' },
        ],
        dynamicTitle: true,
      },
    },
  },
  decorators: [
    (Story, { globals }) => {
      const theme = globals.theme as 'system' | 'light' | 'dark';
      const style = theme === 'light' ? lightPalette : theme === 'dark' ? darkPalette : undefined;
      return (
        <div style={style} className="min-h-dvh bg-canvas p-8">
          <Story />
        </div>
      );
    },
  ],
};

export default preview;

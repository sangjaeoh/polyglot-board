import path from 'node:path';
import js from '@eslint/js';
import nextPlugin from '@next/eslint-plugin-next';
import prettierConfig from 'eslint-config-prettier';
import boundaries from 'eslint-plugin-boundaries';
import jsxA11y from 'eslint-plugin-jsx-a11y';
import reactHooks from 'eslint-plugin-react-hooks';
import tseslint from 'typescript-eslint';
import noRawTailwindValues from './no-raw-tailwind-values.mjs';

// ESLint 강제 기계의 중앙 설정(docs/code-quality.md). 앱·패키지는 여기서 상속만 한다 — 규칙 본문을 복제하지 않는다.
// 엔트리·규칙 파일은 패키지 루트에 둔다 — depcruise의 packages/*/src deep import 금지와 충돌하지 않는 배치다.

/**
 * Next 앱 config. eslint-plugin-boundaries가 FSD 레이어 방향을 강제한다(feature→feature 금지·shared→상위 금지).
 * 워크스페이스 방향(packages→apps 등)은 dependency-cruiser가 소유한다.
 * react-hooks는 recommended-latest로 배선한다 — React Compiler 계열 린트가 여기에만 번들된다(docs/code-quality.md).
 * tsconfigRootDir는 소비자(앱)가 주입한다 — 환경 바인딩이지 규칙 본문이 아니다.
 */
export function nextConfig({ tsconfigRootDir }) {
  return tseslint.config(
    { ignores: ['.next/**', 'next-env.d.ts', 'node_modules/**', 'eslint.config.mjs'] },
    {
      files: ['src/**/*.{ts,tsx}'],
      extends: [
        js.configs.recommended,
        tseslint.configs.recommended,
        reactHooks.configs['recommended-latest'],
        jsxA11y.flatConfigs.recommended,
      ],
      languageOptions: {
        parser: tseslint.parser,
        parserOptions: { projectService: true, tsconfigRootDir },
      },
      plugins: { '@next/next': nextPlugin, boundaries },
      settings: {
        'boundaries/include': ['src/**/*'],
        // Next 관례 파일(src 루트) — FSD 요소가 아니므로 경계 검사에서 제외한다.
        'boundaries/ignore': ['src/instrumentation.ts'],
        'boundaries/elements': [
          { type: 'app', pattern: 'src/app' },
          { type: 'feature', pattern: 'src/features/*', capture: ['slice'] },
          { type: 'entity', pattern: 'src/entities/*', capture: ['slice'] },
          { type: 'shared', pattern: 'src/shared' },
        ],
        'import/resolver': {
          // IDE(cwd=레포 루트)에서도 깨지지 않게 절대 경로로 합성한다.
          typescript: { alwaysTryTypes: true, project: path.join(tsconfigRootDir, 'tsconfig.json') },
        },
      },
      rules: {
        ...nextPlugin.configs.recommended.rules,
        ...nextPlugin.configs['core-web-vitals'].rules,
        // 미등록 파일·미등록 요소 import는 error — widgets·FSD pages 등 레이어 신설을 차단한다.
        'boundaries/no-unknown-files': 'error',
        'boundaries/no-unknown-dependencies': 'error',
        // 레이어 방향(레이어 표 그대로)과 public API 우회 deep import 차단을 한 규칙이 소유한다.
        // 같은 요소 내부 import는 internal로 검사 대상이 아니다(app 내부 조립·슬라이스 내부 자유).
        'boundaries/dependencies': [
          'error',
          {
            default: 'disallow',
            message:
              'FSD 경계 위반: {{ from.element.type }} → {{ to.element.type }} (허용 레이어·public API 경유 확인)',
            policies: [
              {
                from: { element: { type: 'app' } },
                allow: { to: { element: { type: 'feature', fileInternalPath: 'index.{server,client}.ts' } } },
              },
              {
                from: { element: { type: 'feature' } },
                allow: [
                  { to: { element: { type: 'entity', fileInternalPath: 'index.ts' } } },
                  { to: { element: { type: 'shared' } } },
                ],
              },
              { from: { element: { type: 'entity' } }, allow: { to: { element: { type: 'shared' } } } },
              // shared의 의존 대상 "없음"은 정책 부재 + default disallow로 충족된다.
            ],
          },
        ],
      },
    },
    // 토큰 우회 차단(원시 색·치수 값).
    noRawTailwindValues,
    prettierConfig,
  );
}

/**
 * 디자인시스템 워크벤치 config — 토큰 우회 차단만 배선한다.
 * 범위 밖 규칙(react-hooks·jsx-a11y 등)은 넣지 않는다. 워크벤치 빌드 산출물은 린트 대상이 아니다.
 */
export const designSystemConfig = [
  { ignores: ['dist/**', 'storybook-static/**'] },
  {
    files: ['src/**/*.{ts,tsx}'],
    languageOptions: { parser: tseslint.parser },
  },
  noRawTailwindValues,
];

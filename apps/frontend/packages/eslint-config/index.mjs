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
        'boundaries/elements': [
          { type: 'app', pattern: 'src/app', mode: 'folder' },
          { type: 'feature', pattern: 'src/features/*', mode: 'folder', capture: ['slice'] },
          { type: 'entity', pattern: 'src/entities/*', mode: 'folder', capture: ['slice'] },
          { type: 'shared', pattern: 'src/shared', mode: 'folder' },
        ],
        'import/resolver': {
          // IDE(cwd=레포 루트)에서도 깨지지 않게 절대 경로로 합성한다.
          typescript: { alwaysTryTypes: true, project: path.join(tsconfigRootDir, 'tsconfig.json') },
        },
      },
      rules: {
        ...nextPlugin.configs.recommended.rules,
        ...nextPlugin.configs['core-web-vitals'].rules,
        'boundaries/no-unknown-files': 'off',
        'boundaries/no-unknown': 'off',
        'boundaries/element-types': [
          'error',
          {
            default: 'disallow',
            message: 'FSD 레이어 방향 위반: ${file.type} → ${dependency.type}',
            rules: [
              { from: ['app'], allow: ['app', 'feature', 'entity', 'shared'] },
              {
                from: ['feature'],
                allow: [['feature', { slice: '${from.slice}' }], 'entity', 'shared'],
              },
              { from: ['entity'], allow: [['entity', { slice: '${from.slice}' }], 'shared'] },
              { from: ['shared'], allow: ['shared'] },
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

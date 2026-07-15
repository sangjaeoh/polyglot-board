import js from '@eslint/js';
import nextPlugin from '@next/eslint-plugin-next';
import prettierConfig from 'eslint-config-prettier';
import boundaries from 'eslint-plugin-boundaries';
import jsxA11y from 'eslint-plugin-jsx-a11y';
import reactHooks from 'eslint-plugin-react-hooks';
import tseslint from 'typescript-eslint';

// eslint-plugin-boundaries가 FSD 레이어 방향을 강제한다(feature→feature 금지·shared→상위 금지).
// 워크스페이스 방향(packages→apps 등)은 dependency-cruiser가 소유한다.
// react-hooks는 recommended-latest로 배선한다 — React Compiler 계열 린트가 여기에만 번들된다(docs/code-quality.md).
export default tseslint.config(
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
      parserOptions: { projectService: true, tsconfigRootDir: import.meta.dirname },
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
        typescript: { alwaysTryTypes: true, project: './tsconfig.json' },
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
  prettierConfig,
);

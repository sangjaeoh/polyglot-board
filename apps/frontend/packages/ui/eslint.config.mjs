import tseslint from 'typescript-eslint';
import noRawTailwindValues from '../../eslint-rules/no-raw-tailwind-values.mjs';

// 디자인시스템 패키지는 자기 게이트를 소유한다(Storybook 설정 소유와 같은 원칙).
// 이 config는 토큰 우회 차단만 배선한다 — 범위 밖 규칙(react-hooks·jsx-a11y 등)은 넣지 않는다.
// 파서만 세워 src를 파싱하고, 공유 규칙이 원시 색·치수 값을 막는다(code-quality.md ESLint).
export default [
  // 워크벤치 빌드 산출물은 린트 대상이 아니다(번들 코드가 미등록 규칙을 참조한다).
  { ignores: ['dist/**', 'storybook-static/**'] },
  {
    files: ['src/**/*.{ts,tsx}'],
    languageOptions: { parser: tseslint.parser },
  },
  noRawTailwindValues,
];

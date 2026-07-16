// 토큰 우회 차단 규칙(design-system.md 토큰: 원시값은 토큰 정의 스타일시트에만 산다).
// 프론트 서브트리 공유 규칙 — tsconfig.base.json·.dependency-cruiser.cjs와 같은 층에 두고
// 각 워크스페이스의 인라인 flat config가 소비한다(code-quality.md 강제 기계 배치).
//
// 막는 것: Tailwind 임의 값 구문의 색(hex·색 함수)·치수(px), 비토큰 팔레트 색 유틸.
// 막지 않는 것: 색·px가 아닌 임의 값(transition-[prop,...]·supports-[feature]·aspect-[16/9] 등).
// no-restricted-syntax 대신 커스텀 규칙인 이유: 패턴에 문자 클래스([\d.]·\])가 있어
// esquery 속성 정규식 이스케이프가 취약하다. 문자열·템플릿 리터럴을 직접 검사한다(의존성 0).

const PATTERNS = [
  { re: /-\[#[0-9a-fA-F]{3,8}/, label: '임의 값 hex 색' },
  { re: /-\[(?:rgba?|hsla?|oklch|oklab|hwb|lab|lch|color)\(/, label: '임의 값 함수 색' },
  { re: /-\[-?[\d.]+px\]/, label: '임의 값 px 치수' },
  {
    re: /\b(?:bg|text|border|ring-offset|ring|outline|decoration|shadow|caret|accent|fill|stroke|from|via|to|divide|placeholder)-(?:slate|gray|zinc|neutral|stone|red|orange|amber|yellow|lime|green|emerald|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose)-(?:50|100|200|300|400|500|600|700|800|900|950)\b/,
    label: '비토큰 팔레트 색 유틸',
  },
];

const rule = {
  meta: {
    type: 'problem',
    docs: { description: 'Tailwind 클래스의 원시 색·치수 값(토큰 우회)을 금지한다.' },
    schema: [],
    messages: {
      raw: '토큰 우회 금지: {{label}}("{{match}}"). 원시 색·치수 대신 시맨틱 토큰을 쓴다(→ design-system.md 토큰).',
    },
  },
  create(context) {
    const scan = (node, text) => {
      if (typeof text !== 'string') return;
      for (const { re, label } of PATTERNS) {
        const m = re.exec(text);
        if (m) {
          context.report({ node, messageId: 'raw', data: { label, match: m[0] } });
          return;
        }
      }
    };
    return {
      Literal(node) {
        scan(node, node.value);
      },
      TemplateElement(node) {
        scan(node, node.value.raw);
      },
    };
  },
};

// 언어옵션(파서)은 소비 config가 src 블록에서 소유한다 — 여기선 규칙 등록·적용만 한다.
export default {
  name: 'design-tokens/no-raw-tailwind-values',
  files: ['src/**/*.{ts,tsx}'],
  plugins: { 'design-tokens': { rules: { 'no-raw-values': rule } } },
  rules: { 'design-tokens/no-raw-values': 'error' },
};

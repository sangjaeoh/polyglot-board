import { nextConfig } from '@board/eslint-config';

// 규칙 본문은 중앙 설정(@board/eslint-config)이 소유한다(docs/code-quality.md) — 여기는 환경 바인딩만 넘긴다.
export default nextConfig({ tsconfigRootDir: import.meta.dirname });

import { designSystemConfig } from '@board/eslint-config';

// 규칙 본문은 중앙 설정(@board/eslint-config)이 소유한다(docs/code-quality.md).
// Storybook 설정 소유(디자인시스템 패키지)와는 별개다 — ESLint 게이트는 중앙이 소유한다.
export default designSystemConfig;

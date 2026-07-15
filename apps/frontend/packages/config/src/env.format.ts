import type { ZodError } from 'zod';

/** Zod 이슈를 부팅 실패 로그용 사람이 읽는 목록으로 만든다. */
export function formatEnvIssues(error: ZodError): string {
  return error.issues.map((issue) => `- ${issue.path.join('.') || '(root)'}: ${issue.message}`).join('\n');
}

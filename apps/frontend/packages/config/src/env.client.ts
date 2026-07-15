import { z } from 'zod';
import { formatEnvIssues } from './env.format';

// 클라 노출 가능 env(NEXT_PUBLIC_*). Zod로 검증해 raw process.env 접근을 대체한다.
// NEXT_PUBLIC_*은 빌드 시 정적 치환되므로 process.env를 통째로 넘기지 않고 키별로 참조한다.
const clientEnvSchema = z.object({
  NEXT_PUBLIC_SITE_URL: z.string().url().default('http://localhost:3000'),
});

export type ClientEnv = z.infer<typeof clientEnvSchema>;

let cached: ClientEnv | undefined;

/** 클라이언트에 노출 가능한 환경변수를 검증해 반환한다. */
export function getClientEnv(): ClientEnv {
  if (!cached) {
    const parsed = clientEnvSchema.safeParse({
      NEXT_PUBLIC_SITE_URL: process.env.NEXT_PUBLIC_SITE_URL,
    });
    if (!parsed.success) {
      // ZodError를 그대로 던지지 않는다 — message가 getter 전용이라 Next 에러 핸들링이 TypeError로 원인을 가린다.
      throw new Error(`클라 env 검증 실패:\n${formatEnvIssues(parsed.error)}`);
    }
    cached = parsed.data;
  }
  return cached;
}

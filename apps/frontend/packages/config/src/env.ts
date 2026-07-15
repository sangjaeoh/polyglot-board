import { z } from 'zod';
import { formatEnvIssues } from './env.format';

// 서버 전용 env. 접두 없는 값은 서버에서만 읽힌다(클라 노출 경계). 부팅 시 Zod로 검증한다.
// 기본값은 개발 전용이다 — 프로덕션에서 누락이 조용한 localhost 폴백이 되지 않도록 필수화한다.
const backendApiUrl = z.string().url();

const serverEnvSchema = z.object({
  BACKEND_API_URL:
    process.env.NODE_ENV === 'production' ? backendApiUrl : backendApiUrl.default('http://localhost:8080'),
});

export type ServerEnv = z.infer<typeof serverEnvSchema>;

let cached: ServerEnv | undefined;

/** 서버 전용 환경변수를 검증해 반환한다. 내부 URL은 server-only 경계 안에서만 소비한다. */
export function getServerEnv(): ServerEnv {
  if (!cached) {
    const parsed = serverEnvSchema.safeParse(process.env);
    if (!parsed.success) {
      // ZodError를 그대로 던지지 않는다 — message가 getter 전용이라 Next 에러 핸들링이 TypeError로 원인을 가린다.
      throw new Error(`서버 env 검증 실패:\n${formatEnvIssues(parsed.error)}`);
    }
    cached = parsed.data;
  }
  return cached;
}

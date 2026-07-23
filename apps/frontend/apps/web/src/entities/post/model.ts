import type { PostId, PostPageResponse, PostResponse, PostSummary } from '@board/api-client';

export type { PostId, PostResponse, PostSummary, PostPageResponse };

/**
 * ISO-8601 offset 문자열을 UTC 기준 표시 문자열로 바꾼다.
 *
 * <p>로케일·타임존·Date.now 의존을 피해 서버/클라 hydration이 발산하지 않게 결정적으로 포맷한다.
 */
export function formatDateTime(iso: string): string {
  const date = new Date(iso);
  const pad = (n: number): string => String(n).padStart(2, '0');
  return (
    `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())}` +
    ` ${pad(date.getUTCHours())}:${pad(date.getUTCMinutes())} UTC`
  );
}

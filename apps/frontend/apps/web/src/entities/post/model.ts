import {
  postIdSchema,
  postResponseSchema,
  postPageResponseSchema,
  type PostId,
  type PostResponse,
  type PostSummary,
  type PostPageResponse,
} from 'shared-types';

// 프론트 read-model은 백엔드 진실의 투영이다. 스키마는 계약에서 생성된 것을 재노출하고, 순수 뷰 파생만 더한다.
export { postIdSchema, postResponseSchema, postPageResponseSchema };
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

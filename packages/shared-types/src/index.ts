import { z } from 'zod';
import {
  getPostResponse,
  listPostsResponse,
  createPostBody,
  updatePostBody,
  listPostsQueryParams,
} from './generated/zod';

// 계약(openapi.json)에서 생성된 Zod를 벤더 중립 이름으로 재노출한다. 타입은 z.infer로 파생(단일 소스).
// create는 201이라 orval이 응답 스키마를 생성하지 않으나 형상이 상세(getPostResponse)와 같아 재사용한다.
export const postResponseSchema = getPostResponse;
export const postPageResponseSchema = listPostsResponse;
export const postCreateRequestSchema = createPostBody;
export const postUpdateRequestSchema = updatePostBody;
export const postListQuerySchema = listPostsQueryParams;

// ProblemDetail은 400/404/500 응답에 참조되지만 orval zod는 에러 응답 스키마를 생성하지 않는다.
// 계약(components.schemas.ProblemDetail)의 고정 형상을 이 seam 패키지가 소유한다. code는 open string.
export const problemDetailSchema = z.object({
  type: z.string().optional(),
  title: z.string(),
  status: z.number(),
  detail: z.string().optional(),
  instance: z.string().optional(),
  code: z.string(),
  errors: z
    .array(z.object({ field: z.string(), message: z.string() }))
    .optional(),
  // 관측용 확장 멤버 — 로깅·관측에 쓰되 클라 UI에 raw로 노출하지 않는다.
  traceId: z.string().optional(),
});

export type PostResponse = z.infer<typeof postResponseSchema>;
// 주의(타입이 거짓말하는 지점): PageResponse.totalElements는 계약상 int64다. TS number는 2^53 초과에서
// 정밀도를 잃는다. 게시판 규모에선 도달 불가라 number로 둔다 — 대규모 카운트면 string/bigint로 승격한다.
export type PostPageResponse = z.infer<typeof postPageResponseSchema>;
export type PostSummary = PostPageResponse['content'][number];
export type PostCreateRequest = z.infer<typeof postCreateRequestSchema>;
export type PostUpdateRequest = z.infer<typeof postUpdateRequestSchema>;
export type PostListQuery = z.infer<typeof postListQuerySchema>;
export type ProblemDetail = z.infer<typeof problemDetailSchema>;

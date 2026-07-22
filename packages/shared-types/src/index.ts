import { z } from 'zod';
import {
  getPostResponse,
  listPostsResponse,
  createPostBody,
  updatePostBody,
  listPostsQueryParams,
} from './generated/zod';

// UUIDv7 id: plain string + 브랜드 타입·format 검증(docs/sharing.md 타입 정직성 — 생성 타입만으로 신뢰하지 않음).
// .uuid()는 형식 검증이며 v7 버전 자체는 검증하지 않는다(버전 보증은 백엔드 생성기 소유).
export const postIdSchema = z.string().uuid().brand<'PostId'>();
export type PostId = z.infer<typeof postIdSchema>;

// 계약(openapi.json)에서 생성된 Zod를 벤더 중립 이름으로 재노출한다. 타입은 z.infer로 파생(단일 소스).
// create는 201이라 orval이 응답 스키마를 생성하지 않으나 형상이 상세(getPostResponse)와 같아 재사용한다.
// id 필드는 seam에서 브랜드로 승격한다 — egress 검증을 통과한 값만 PostId가 된다.
export const postResponseSchema = getPostResponse.extend({ id: postIdSchema });
export const postPageResponseSchema = listPostsResponse.extend({
  content: z
    .array(listPostsResponse.shape.content.element.extend({ id: postIdSchema }))
    .describe('페이지 항목 목록'),
});
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
// int64(totalElements)는 계약이 string으로 표현한다(docs/sharing.md 표) — JSON number의 2^53 정밀도
// 손실은 wire에서 발생하므로 소비 측 변환이 아닌 계약 표현이 진짜 해결이다. 생성 스키마가 z.string()이다.
export type PostPageResponse = z.infer<typeof postPageResponseSchema>;
export type PostSummary = PostPageResponse['content'][number];
export type PostCreateRequest = z.infer<typeof postCreateRequestSchema>;
export type PostUpdateRequest = z.infer<typeof postUpdateRequestSchema>;
export type PostListQuery = z.infer<typeof postListQuerySchema>;
export type ProblemDetail = z.infer<typeof problemDetailSchema>;

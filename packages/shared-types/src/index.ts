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
// id 브랜드 승격(UUIDv7 타입 정직성, docs/sharing.md)은 프론트 소비 계층(api-client)이 소유한다 —
// 이 패키지는 계약에서 생성된 산출물만 포함한다(docs/architecture.md).
export const postResponseSchema = getPostResponse;
export const postPageResponseSchema = listPostsResponse;
export const postCreateRequestSchema = createPostBody;
export const postUpdateRequestSchema = updatePostBody;
export const postListQuerySchema = listPostsQueryParams;

export type PostResponse = z.infer<typeof postResponseSchema>;
// int64(totalElements)는 계약이 string으로 표현한다(docs/sharing.md 표) — JSON number의 2^53 정밀도
// 손실은 wire에서 발생하므로 소비 측 변환이 아닌 계약 표현이 진짜 해결이다. 생성 스키마가 z.string()이다.
export type PostPageResponse = z.infer<typeof postPageResponseSchema>;
export type PostSummary = PostPageResponse['content'][number];
export type PostCreateRequest = z.infer<typeof postCreateRequestSchema>;
export type PostUpdateRequest = z.infer<typeof postUpdateRequestSchema>;
export type PostListQuery = z.infer<typeof postListQuerySchema>;

export * from './generated/problem-detail';

import { z } from 'zod';
import {
  postResponseSchema as rawPostResponseSchema,
  postPageResponseSchema as rawPostPageResponseSchema,
} from 'shared-types';

// UUIDv7 id: plain string + 브랜드 타입·format 검증(docs/sharing.md 타입 정직성 — 생성 타입만으로 신뢰하지 않음).
// .uuid()는 형식 검증이며 v7 버전 자체는 검증하지 않는다(버전 보증은 백엔드 생성기 소유).
// 이 브랜드 승격은 프론트 소비 계층(api-client)이 소유한다 — 루트 shared-types는 생성물만 포함한다.
export const postIdSchema = z.string().uuid().brand<'PostId'>();
export type PostId = z.infer<typeof postIdSchema>;

export const postResponseSchema = rawPostResponseSchema.extend({ id: postIdSchema });
export const postPageResponseSchema = rawPostPageResponseSchema.extend({
  content: z
    .array(rawPostPageResponseSchema.shape.content.element.extend({ id: postIdSchema }))
    .describe('페이지 항목 목록'),
});

export type PostResponse = z.infer<typeof postResponseSchema>;
export type PostPageResponse = z.infer<typeof postPageResponseSchema>;
export type PostSummary = PostPageResponse['content'][number];

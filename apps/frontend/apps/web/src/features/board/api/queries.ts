// 서버 전용 세그먼트 자체 가드 — api-client의 전이 가드에 기대지 않는다(리팩터로 조용히 소멸 가능).
import 'server-only';

import { cache } from 'react';
import { cacheLife, cacheTag } from 'next/cache';
import { getPost as fetchPost, getPosts as fetchPosts } from '@board/api-client';
import { postListTag, postTag } from '@/entities/post';
import type { PostId } from '@board/api-client';

// 백엔드 호출은 api-client(server-only)로만 한다. feature가 앱에 노출하는 read 경계다.
async function cachedGetPosts(params: { page?: number; size?: number } = {}) {
  'use cache';
  cacheTag(postListTag());
  cacheLife('minutes');
  return fetchPosts(params);
}

export const getPosts = cachedGetPosts;

// 상세는 페이지·generateMetadata가 함께 호출하므로 요청 단위로도 메모이즈한다.
async function cachedGetPost(id: PostId) {
  'use cache';
  cacheTag(postTag(id));
  cacheLife('minutes');
  return fetchPost(id);
}

export const getPost = cache(cachedGetPost);

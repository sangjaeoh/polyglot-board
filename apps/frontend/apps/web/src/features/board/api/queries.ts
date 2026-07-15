import { cache } from 'react';
import { getPost as fetchPost, getPosts as fetchPosts } from '@board/api-client';

// 백엔드 호출은 api-client(server-only)로만 한다. feature가 앱에 노출하는 read 경계다.
export const getPosts = fetchPosts;

// 상세는 페이지·generateMetadata가 함께 호출하므로 요청 단위로 메모이즈한다.
export const getPost = cache(fetchPost);

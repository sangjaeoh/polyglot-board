import 'server-only';

import { getServerEnv } from '@board/config/server';
import {
  postPageResponseSchema,
  postResponseSchema,
  problemDetailSchema,
  type PostCreateRequest,
  type PostId,
  type PostPageResponse,
  type PostResponse,
  type PostUpdateRequest,
} from 'shared-types';
import { z, type ZodType, type ZodTypeDef } from 'zod';
import { ApiError } from './error';

// 백엔드 호출은 이 타입드 클라이언트(server-only)로만 한다. base URL·헤더는 여기 경계 안에 둔다.
function url(path: string): string {
  return `${getServerEnv().BACKEND_API_URL}${path}`;
}

const jsonHeaders = {
  'Content-Type': 'application/json',
  Accept: 'application/json',
};

async function toApiError(response: Response): Promise<ApiError> {
  let body: unknown;
  try {
    body = await response.json();
  } catch {
    body = undefined;
  }
  const parsed = problemDetailSchema.safeParse(body);
  if (parsed.success) {
    return new ApiError(parsed.data);
  }
  return new ApiError({ title: 'UNKNOWN_ERROR', status: response.status, code: 'UNKNOWN_ERROR' });
}

// egress(백엔드→BFF) 검증은 이 경계 1곳에서 safeParse한다.
// 상세(단일 리소스)는 실패 시 throw한다 — 드롭할 국소 단위가 없다(docs/data.md의 예외).
async function readValidated<T>(response: Response, schema: ZodType<T, ZodTypeDef, unknown>): Promise<T> {
  const json: unknown = await response.json();
  const parsed = schema.safeParse(json);
  if (!parsed.success) {
    console.error('[api-client] egress 검증 실패', parsed.error.issues);
    throw new ApiError({ title: 'EGRESS_VALIDATION_FAILED', status: 502, code: 'EGRESS_VALIDATION_FAILED' });
  }
  return parsed.data;
}

// 목록 egress는 페이지 봉투를 검증한 뒤 content 항목을 개별 safeParse한다.
// 불량 항목은 드롭하고 서버 로그로 남긴다 — 레코드 하나가 페이지 전체를 throw로 만들지 않는다.
const postPageEnvelopeSchema = postPageResponseSchema.extend({ content: z.array(z.unknown()) });
const postSummarySchema = postPageResponseSchema.shape.content.element;

async function readValidatedPage(response: Response): Promise<PostPageResponse> {
  const envelope = await readValidated(response, postPageEnvelopeSchema);
  const content: PostPageResponse['content'] = [];
  for (const item of envelope.content) {
    const parsed = postSummarySchema.safeParse(item);
    if (parsed.success) {
      content.push(parsed.data);
    } else {
      console.error('[api-client] egress 목록 항목 검증 실패 — 항목 드롭', parsed.error.issues);
    }
  }
  return { ...envelope, content };
}

/** 활성 게시글 목록을 조회한다. */
export async function getPosts(params: { page?: number; size?: number } = {}): Promise<PostPageResponse> {
  const search = new URLSearchParams();
  if (params.page !== undefined) search.set('page', String(params.page));
  if (params.size !== undefined) search.set('size', String(params.size));
  const response = await fetch(url(`/api/v1/posts?${search.toString()}`), {
    cache: 'no-store',
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) throw await toApiError(response);
  return readValidatedPage(response);
}

export async function getPost(id: PostId): Promise<PostResponse> {
  const response = await fetch(url(`/api/v1/posts/${encodeURIComponent(id)}`), {
    cache: 'no-store',
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) throw await toApiError(response);
  return readValidated(response, postResponseSchema);
}

export async function createPost(body: PostCreateRequest): Promise<PostResponse> {
  const response = await fetch(url('/api/v1/posts'), {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(body),
  });
  if (!response.ok) throw await toApiError(response);
  return readValidated(response, postResponseSchema);
}

export async function updatePost(id: PostId, body: PostUpdateRequest): Promise<PostResponse> {
  const response = await fetch(url(`/api/v1/posts/${encodeURIComponent(id)}`), {
    method: 'PUT',
    headers: jsonHeaders,
    body: JSON.stringify(body),
  });
  if (!response.ok) throw await toApiError(response);
  return readValidated(response, postResponseSchema);
}

export async function deletePost(id: PostId): Promise<void> {
  const response = await fetch(url(`/api/v1/posts/${encodeURIComponent(id)}`), {
    method: 'DELETE',
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) throw await toApiError(response);
}

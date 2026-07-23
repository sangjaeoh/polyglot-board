import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { createPost, deletePost, getPost, getPosts, updatePost } from './client';
import { postIdSchema } from './schemas';

const BASE_URL = 'http://localhost:8080';
const POST_ID = postIdSchema.parse('01912d68-7b3a-7000-8000-000000000001');

const VALID_POST = {
  id: POST_ID,
  title: '제목',
  content: '본문',
  author: '글쓴이',
  createdAt: '2026-07-23T00:00:00Z',
  updatedAt: '2026-07-23T00:00:00Z',
};

const server = setupServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('getPost', () => {
  it('유효한 상세 응답을 검증해 반환한다', async () => {
    server.use(http.get(`${BASE_URL}/api/v1/posts/:id`, () => HttpResponse.json(VALID_POST)));

    const post = await getPost(POST_ID);

    expect(post.title).toBe('제목');
    expect(post.id).toBe(POST_ID);
  });

  it('404 응답을 ApiError로 변환해 던진다', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/posts/:id`, () =>
        HttpResponse.json(
          { title: '없음', status: 404, code: 'POST_NOT_FOUND' },
          { status: 404 },
        ),
      ),
    );

    await expect(getPost(POST_ID)).rejects.toMatchObject({ code: 'POST_NOT_FOUND' });
  });

  it('egress 검증에 실패하는 응답은 던진다', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    server.use(http.get(`${BASE_URL}/api/v1/posts/:id`, () => HttpResponse.json({ id: POST_ID })));

    await expect(getPost(POST_ID)).rejects.toMatchObject({ code: 'EGRESS_VALIDATION_FAILED' });
  });
});

describe('getPosts', () => {
  it('불량 항목은 드롭하고 정상 항목만 반환한다', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    server.use(
      http.get(`${BASE_URL}/api/v1/posts`, () =>
        HttpResponse.json({
          content: [
            { id: POST_ID, title: '정상', author: '글쓴이', createdAt: '2026-07-23T00:00:00Z' },
            { id: 'not-a-uuid' },
          ],
          page: 1,
          pageSize: 20,
          totalElements: '2',
          totalPages: 1,
        }),
      ),
    );

    const page = await getPosts();

    expect(page.content).toHaveLength(1);
    expect(page.content[0]?.title).toBe('정상');
  });
});

describe('createPost/updatePost/deletePost', () => {
  it('createPost는 생성된 게시글을 반환한다', async () => {
    server.use(http.post(`${BASE_URL}/api/v1/posts`, () => HttpResponse.json(VALID_POST, { status: 201 })));

    const created = await createPost({ title: '제목', content: '본문', author: '글쓴이' });

    expect(created.id).toBe(POST_ID);
  });

  it('updatePost는 수정된 게시글을 반환한다', async () => {
    server.use(http.put(`${BASE_URL}/api/v1/posts/:id`, () => HttpResponse.json(VALID_POST)));

    const updated = await updatePost(POST_ID, { title: '제목', content: '본문' });

    expect(updated.id).toBe(POST_ID);
  });

  it('deletePost는 204에서 정상 완료한다', async () => {
    server.use(http.delete(`${BASE_URL}/api/v1/posts/:id`, () => new HttpResponse(null, { status: 204 })));

    await expect(deletePost(POST_ID)).resolves.toBeUndefined();
  });
});

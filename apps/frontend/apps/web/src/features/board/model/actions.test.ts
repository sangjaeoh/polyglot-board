// @vitest-environment node
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { revalidatePath } from 'next/cache';
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { createPostAction, deletePostAction, updatePostAction } from './actions';

const BASE_URL = 'http://localhost:8080';

const VALID_POST = {
  id: '01912d68-7b3a-7000-8000-000000000001',
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

beforeEach(() => {
  vi.mocked(revalidatePath).mockClear();
});

function formData(fields: Record<string, string>): FormData {
  const data = new FormData();
  for (const [key, value] of Object.entries(fields)) data.set(key, value);
  return data;
}

describe('createPostAction', () => {
  it('입력이 비면 필드 에러를 반환하고 캐시를 무효화하지 않는다', async () => {
    const state = await createPostAction(null, formData({ title: '', content: '', author: '' }));

    expect(state.fieldErrors).toBeDefined();
    expect(revalidatePath).not.toHaveBeenCalled();
  });

  it('작성에 성공하면 캐시를 무효화하고 상세로 리다이렉트한다', async () => {
    server.use(http.post(`${BASE_URL}/api/v1/posts`, () => HttpResponse.json(VALID_POST, { status: 201 })));

    await expect(
      createPostAction(null, formData({ title: '제목', content: '본문', author: '글쓴이' })),
    ).rejects.toThrow(`NEXT_REDIRECT:/posts/${VALID_POST.id}`);
    expect(revalidatePath).toHaveBeenCalledWith('/');
  });
});

describe('updatePostAction', () => {
  it('id가 유효하지 않으면 메시지를 반환하고 캐시를 무효화하지 않는다', async () => {
    const state = await updatePostAction('invalid-id', null, formData({ title: '제목', content: '본문' }));

    expect(state.message).toBe('잘못된 요청입니다.');
    expect(revalidatePath).not.toHaveBeenCalled();
  });
});

describe('deletePostAction', () => {
  it('삭제에 성공하면 캐시를 무효화하고 목록으로 리다이렉트한다', async () => {
    server.use(http.delete(`${BASE_URL}/api/v1/posts/:id`, () => new HttpResponse(null, { status: 204 })));

    await expect(deletePostAction(VALID_POST.id)).rejects.toThrow('NEXT_REDIRECT:/');
    expect(revalidatePath).toHaveBeenCalledWith('/');
  });

  it('이미 삭제된 게시글이면 에러를 삼키고 목록으로 리다이렉트한다', async () => {
    server.use(
      http.delete(`${BASE_URL}/api/v1/posts/:id`, () =>
        HttpResponse.json({ title: '없음', status: 404, code: 'POST_NOT_FOUND' }, { status: 404 }),
      ),
    );

    await expect(deletePostAction(VALID_POST.id)).rejects.toThrow('NEXT_REDIRECT:/');
    expect(revalidatePath).toHaveBeenCalledWith('/');
  });
});

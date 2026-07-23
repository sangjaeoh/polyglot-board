// @vitest-environment node
import { redirect } from 'next/navigation';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@board/api-client';

vi.mock('next/navigation', () => ({ redirect: vi.fn() }));
vi.mock('next/cache', () => ({ revalidatePath: vi.fn() }));
vi.mock('@board/api-client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@board/api-client')>();
  return {
    ...actual,
    createPost: vi.fn(),
    updatePost: vi.fn(),
    deletePost: vi.fn(),
  };
});

const { createPost, updatePost, deletePost } = await import('@board/api-client');
const { createPostAction, updatePostAction, deletePostAction } = await import('./actions');

const VALID_POST = {
  id: '01912d68-7b3a-7000-8000-000000000001',
  title: '제목',
  content: '본문',
  author: '글쓴이',
  createdAt: '2026-07-23T00:00:00Z',
  updatedAt: '2026-07-23T00:00:00Z',
};

function formData(fields: Record<string, string>): FormData {
  const data = new FormData();
  for (const [key, value] of Object.entries(fields)) data.set(key, value);
  return data;
}

beforeEach(() => {
  vi.mocked(redirect).mockClear();
});

describe('createPostAction', () => {
  it('입력이 비면 필드 에러를 반환하고 백엔드를 호출하지 않는다', async () => {
    const state = await createPostAction(null, formData({ title: '', content: '', author: '' }));

    expect(state.fieldErrors).toBeDefined();
    expect(createPost).not.toHaveBeenCalled();
  });

  it('작성에 성공하면 상세로 리다이렉트한다', async () => {
    vi.mocked(createPost).mockResolvedValue(VALID_POST as never);

    await createPostAction(null, formData({ title: '제목', content: '본문', author: '글쓴이' }));

    expect(redirect).toHaveBeenCalledWith(`/posts/${VALID_POST.id}`);
  });
});

describe('updatePostAction', () => {
  it('id가 유효하지 않으면 메시지를 반환한다', async () => {
    const state = await updatePostAction('invalid-id', null, formData({ title: '제목', content: '본문' }));

    expect(state.message).toBe('잘못된 요청입니다.');
    expect(updatePost).not.toHaveBeenCalled();
  });
});

describe('deletePostAction', () => {
  it('삭제에 성공하면 목록으로 리다이렉트한다', async () => {
    vi.mocked(deletePost).mockResolvedValue(undefined);

    await deletePostAction(VALID_POST.id);

    expect(redirect).toHaveBeenCalledWith('/');
  });

  it('이미 삭제된 게시글이면 에러를 삼키고 목록으로 리다이렉트한다', async () => {
    vi.mocked(deletePost).mockRejectedValue(new ApiError({ title: '없음', status: 404, code: 'POST_NOT_FOUND' }));

    await deletePostAction(VALID_POST.id);

    expect(redirect).toHaveBeenCalledWith('/');
  });
});

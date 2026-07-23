import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { PostId } from '@/entities/post';
import { PostList } from './PostList';

vi.mock('next/link', () => ({
  default: ({ href, children, ...props }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...props}>
      {children}
    </a>
  ),
}));

describe('PostList', () => {
  it('항목이 없으면 빈 상태를 보여준다', () => {
    render(
      <PostList page={{ content: [], page: 1, pageSize: 20, totalElements: '0', totalPages: 0 }} />,
    );

    expect(screen.getByText('아직 글이 없습니다')).toBeInTheDocument();
  });

  it('항목이 있으면 목록과 페이지 위치를 보여준다', () => {
    render(
      <PostList
        page={{
          content: [
            {
              id: '01912d68-7b3a-7000-8000-000000000001' as PostId,
              title: '첫 글',
              author: '글쓴이',
              createdAt: '2026-07-23T05:07:00Z',
            },
          ],
          page: 2,
          pageSize: 20,
          totalElements: '21',
          totalPages: 2,
        }}
      />,
    );

    expect(screen.getByText('첫 글')).toBeInTheDocument();
    expect(screen.getByText('2 / 2')).toBeInTheDocument();
  });
});

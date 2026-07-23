import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { PostId } from '@/entities/post';
import { PostCard } from './PostCard';

vi.mock('next/link', () => ({
  default: ({ href, children, ...props }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...props}>
      {children}
    </a>
  ),
}));

const SUMMARY = {
  id: '01912d68-7b3a-7000-8000-000000000001' as PostId,
  title: '첫 글',
  author: '글쓴이',
  createdAt: '2026-07-23T05:07:00Z',
};

describe('PostCard', () => {
  it('제목·작성자를 표시하고 상세 링크로 연결한다', () => {
    render(<PostCard post={SUMMARY} />);

    expect(screen.getByText('첫 글')).toBeInTheDocument();
    expect(screen.getByText('글쓴이')).toBeInTheDocument();
    expect(screen.getByRole('link')).toHaveAttribute('href', `/posts/${SUMMARY.id}`);
  });
});

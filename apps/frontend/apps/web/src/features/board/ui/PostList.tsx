import Link from 'next/link';
import type { PostPageResponse } from '@/entities/post';
import { PostCard } from './PostCard';

export function PostList({ page }: { page: PostPageResponse }) {
  if (page.content.length === 0) {
    return (
      <div className="flex flex-col items-center gap-5 rounded-2xl border border-dashed border-edge-strong bg-surface px-6 py-16 text-center shadow-card">
        <span aria-hidden className="h-8 w-[3px] rounded-full bg-accent" />
        <div className="flex flex-col gap-1.5">
          <p className="text-emphasis text-ink">아직 글이 없습니다</p>
          <p className="text-caption text-muted">첫 글을 남겨 이 게시판을 시작하세요.</p>
        </div>
        <Link
          href="/posts/new"
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-label text-on-primary shadow-control transition-colors duration-fast hover:bg-primary-hover focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
        >
          새 글 작성
        </Link>
      </div>
    );
  }

  const hasPrev = page.page > 0;
  const hasNext = page.page + 1 < page.totalPages;
  const pageLinkClass =
    'inline-flex items-center gap-1.5 rounded-lg border border-edge bg-surface px-3.5 py-2 text-label text-ink shadow-control transition-colors duration-fast hover:border-edge-strong hover:text-accent focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent';
  const pageDisabledClass =
    'inline-flex items-center gap-1.5 rounded-lg border border-edge px-3.5 py-2 text-label text-faint';

  return (
    <div className="flex flex-col gap-6">
      <ul className="flex flex-col gap-3">
        {page.content.map((post) => (
          <PostCard key={post.id} post={post} />
        ))}
      </ul>
      <nav className="flex items-center justify-between" aria-label="페이지네이션">
        {hasPrev ? (
          <Link href={`/?page=${page.page - 1}`} className={pageLinkClass}>
            <span aria-hidden>←</span> 이전
          </Link>
        ) : (
          <span className={pageDisabledClass}>
            <span aria-hidden>←</span> 이전
          </span>
        )}
        <span className="font-mono text-meta text-muted tabular-nums">
          {page.page + 1} / {Math.max(page.totalPages, 1)}
        </span>
        {hasNext ? (
          <Link href={`/?page=${page.page + 1}`} className={pageLinkClass}>
            다음 <span aria-hidden>→</span>
          </Link>
        ) : (
          <span className={pageDisabledClass}>
            다음 <span aria-hidden>→</span>
          </span>
        )}
      </nav>
    </div>
  );
}

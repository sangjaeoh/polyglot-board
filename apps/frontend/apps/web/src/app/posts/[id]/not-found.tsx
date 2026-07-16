import Link from 'next/link';

export default function PostNotFound() {
  return (
    <div className="flex flex-col items-start gap-5 py-8">
      <span aria-hidden className="h-8 w-rail rounded-full bg-accent" />
      <div className="flex flex-col gap-1.5">
        <p className="text-title-sm text-ink">게시글을 찾을 수 없습니다</p>
        <p className="text-caption text-muted">
          이미 삭제되었거나 주소가 올바르지 않을 수 있습니다.
        </p>
      </div>
      <Link
        href="/"
        className="inline-flex items-center gap-2 rounded-lg border border-edge-strong bg-surface px-4 py-2.5 text-label text-ink shadow-control transition-colors duration-fast hover:bg-surface-hover focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
      >
        <span aria-hidden>←</span> 목록으로 돌아가기
      </Link>
    </div>
  );
}

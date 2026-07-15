import Link from 'next/link';

export default function NotFound() {
  return (
    <div className="flex flex-col items-start gap-5 py-8">
      <span aria-hidden className="h-8 w-[3px] rounded-full bg-accent" />
      <div className="flex flex-col gap-1.5">
        <p className="font-mono text-xs text-accent tabular-nums">404</p>
        <p className="text-lg font-semibold text-ink">페이지를 찾을 수 없습니다</p>
        <p className="text-sm text-muted">주소가 바뀌었거나 삭제된 페이지일 수 있습니다.</p>
      </div>
      <Link
        href="/"
        className="inline-flex items-center gap-2 rounded-lg border border-edge-strong bg-surface px-4 py-2.5 text-sm font-medium text-ink shadow-control transition-colors duration-150 hover:bg-surface-hover focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
      >
        <span aria-hidden>←</span> 목록으로 돌아가기
      </Link>
    </div>
  );
}

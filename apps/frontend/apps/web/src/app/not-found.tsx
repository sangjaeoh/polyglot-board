import Link from 'next/link';
import { buttonClass } from '@board/ui';

export default function NotFound() {
  return (
    <div className="flex flex-col items-start gap-5 py-8">
      <span aria-hidden className="h-8 w-rail rounded-full bg-accent" />
      <div className="flex flex-col gap-1.5">
        <p className="font-mono text-meta text-accent tabular-nums">404</p>
        <p className="text-title-sm text-ink">페이지를 찾을 수 없습니다</p>
        <p className="text-caption text-muted">주소가 바뀌었거나 삭제된 페이지일 수 있습니다.</p>
      </div>
      <Link href="/" className={buttonClass('secondary')}>
        <span aria-hidden>←</span> 목록으로 돌아가기
      </Link>
    </div>
  );
}

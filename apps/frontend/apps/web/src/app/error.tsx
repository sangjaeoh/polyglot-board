'use client';

import { Button } from '@board/ui';

// 에러 바운더리는 클라에서 렌더된다. 부분 실패를 국소 degrade한다.
export default function Error({
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="flex flex-col items-start gap-4 rounded-xl border border-danger-text/25 bg-danger-soft px-5 py-6">
      <div className="flex flex-col gap-1.5">
        <p className="text-label text-danger-text">콘텐츠를 불러오지 못했습니다</p>
        <p className="text-caption text-muted">잠시 후 다시 시도하거나 페이지를 새로고침하세요.</p>
      </div>
      <Button variant="secondary" onClick={reset}>
        다시 시도
      </Button>
    </div>
  );
}

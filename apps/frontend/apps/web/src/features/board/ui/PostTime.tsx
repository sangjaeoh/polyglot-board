'use client';

import { useEffect, useState } from 'react';
import { formatDateTime } from '@/entities/post';

function formatLocal(iso: string): string {
  const date = new Date(iso);
  const pad = (n: number): string => String(n).padStart(2, '0');
  const ymd = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  const hm = `${pad(date.getHours())}:${pad(date.getMinutes())}`;
  // 로컬 타임존 약어(브라우저·로케일에 따라 KST 또는 GMT+9 형태).
  const tz = new Intl.DateTimeFormat(undefined, { timeZoneName: 'short' })
    .formatToParts(date)
    .find((part) => part.type === 'timeZoneName')?.value;
  return tz ? `${ymd} ${hm} ${tz}` : `${ymd} ${hm}`;
}

/**
 * 게시글 시각 표시. 서버·초기 렌더는 결정적 UTC로 하이드레이션 불일치를 피하고,
 * 마운트 후 클라 effect에서 사용자 로컬 타임존으로 교체한다.
 */
export function PostTime({ iso, className }: { iso: string; className?: string }) {
  const [display, setDisplay] = useState(() => formatDateTime(iso));

  useEffect(() => {
    setDisplay(formatLocal(iso));
  }, [iso]);

  return (
    <time dateTime={iso} className={className}>
      {display}
    </time>
  );
}

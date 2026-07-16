import Link from 'next/link';
import { z } from 'zod';
import { DEFAULT_PAGE_SIZE } from '@board/config';
import { buttonClass } from '@board/ui';
import { getPosts, PostList } from '@/features/board/index.server';

// ingress(searchParam)는 엄격 Zod로 검증한다. 잘못된 값은 0페이지로 보정한다.
const pageParamSchema = z.coerce.number().int().min(0).catch(0);

export default async function HomePage({
  searchParams,
}: {
  searchParams: Promise<{ page?: string }>;
}) {
  const { page: pageParam } = await searchParams;
  const page = pageParamSchema.parse(pageParam);
  const data = await getPosts({ page, size: DEFAULT_PAGE_SIZE });

  return (
    <div className="flex flex-col gap-8">
      <div className="flex items-end justify-between gap-4">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-title-md text-ink">게시글</h1>
          <p className="font-mono text-meta text-muted tabular-nums">총 {data.totalElements}개</p>
        </div>
        <Link href="/posts/new" className={buttonClass('primary', 'shrink-0')}>
          새 글 작성
        </Link>
      </div>
      <PostList page={data} />
    </div>
  );
}

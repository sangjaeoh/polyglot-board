import Link from 'next/link';
import { z } from 'zod';
import { DEFAULT_PAGE_SIZE } from '@board/config';
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
          <h1 className="text-2xl font-semibold tracking-tight text-ink">게시글</h1>
          <p className="font-mono text-xs text-muted tabular-nums">총 {data.totalElements}개</p>
        </div>
        <Link
          href="/posts/new"
          className="inline-flex shrink-0 items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-on-primary shadow-control transition-colors duration-150 hover:bg-primary-hover focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
        >
          새 글 작성
        </Link>
      </div>
      <PostList page={data} />
    </div>
  );
}

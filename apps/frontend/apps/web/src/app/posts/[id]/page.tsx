import type { Metadata } from 'next';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { ApiError, getPost } from '@/features/board/index.server';
import { DeletePostButton, PostTime } from '@/features/board/index.client';
import { type PostResponse } from '@/entities/post';

async function loadPost(id: string): Promise<PostResponse> {
  try {
    return await getPost(id);
  } catch (error) {
    if (error instanceof ApiError && error.code === 'POST_NOT_FOUND') notFound();
    throw error;
  }
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ id: string }>;
}): Promise<Metadata> {
  const { id } = await params;
  try {
    const post = await getPost(id);
    return { title: post.title };
  } catch {
    return { title: '게시글' };
  }
}

export default async function PostDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const post = await loadPost(id);
  const wasEdited = post.updatedAt !== post.createdAt;

  return (
    <article className="flex flex-col gap-8">
      <header className="flex flex-col gap-3 border-l-2 border-accent pl-5">
        <h1 className="text-3xl font-semibold leading-tight tracking-tight text-balance text-ink">
          {post.title}
        </h1>
        <p className="flex flex-wrap items-center gap-x-2 gap-y-1 text-sm">
          <span className="font-medium text-muted">{post.author}</span>
          <span aria-hidden className="text-faint">
            ·
          </span>
          <PostTime iso={post.createdAt} className="font-mono text-xs text-faint" />
          {wasEdited ? (
            <>
              <span aria-hidden className="text-faint">
                ·
              </span>
              <span className="font-mono text-xs text-faint">
                수정 <PostTime iso={post.updatedAt} />
              </span>
            </>
          ) : null}
        </p>
      </header>
      <div className="max-w-prose text-base leading-8 whitespace-pre-wrap text-ink">
        {post.content}
      </div>
      <div className="flex items-center gap-3 border-t border-edge pt-6">
        <Link
          href={`/posts/${post.id}/edit`}
          className="inline-flex items-center gap-2 rounded-lg border border-edge-strong bg-surface px-4 py-2.5 text-sm font-medium text-ink shadow-control transition-colors duration-150 hover:bg-surface-hover focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
        >
          수정
        </Link>
        <DeletePostButton id={post.id} />
        <Link
          href="/"
          className="ml-auto text-sm font-medium text-muted transition-colors duration-150 hover:text-ink focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
        >
          목록
        </Link>
      </div>
    </article>
  );
}

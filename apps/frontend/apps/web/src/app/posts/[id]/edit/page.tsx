import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { ApiError, getPost, updatePostAction } from '@/features/board/index.server';
import { PostForm } from '@/features/board/index.client';

export const metadata: Metadata = { title: '게시글 수정' };

export default async function EditPostPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;

  let post;
  try {
    post = await getPost(id);
  } catch (error) {
    if (error instanceof ApiError && error.code === 'POST_NOT_FOUND') notFound();
    throw error;
  }

  const action = updatePostAction.bind(null, post.id);

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-1.5">
        <h1 className="text-2xl font-semibold tracking-tight text-ink">게시글 수정</h1>
        <p className="text-sm text-muted">제목과 내용을 고쳐 저장하세요.</p>
      </div>
      <PostForm
        action={action}
        submitLabel="수정"
        initial={{ title: post.title, content: post.content }}
      />
    </div>
  );
}

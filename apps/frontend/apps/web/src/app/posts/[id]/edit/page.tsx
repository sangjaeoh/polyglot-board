import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { ApiError, getPost, postIdSchema, updatePostAction } from '@/features/board/index.server';
import { PostForm } from '@/features/board/index.client';

export const metadata: Metadata = { title: '게시글 수정' };

export default async function EditPostPage({ params }: { params: Promise<{ id: string }> }) {
  const { id: rawId } = await params;
  // ingress(라우트 파라미터)는 엄격 Zod 검증한다 — 형식 불량 id는 404.
  const parsedId = postIdSchema.safeParse(rawId);
  if (!parsedId.success) notFound();

  let post;
  try {
    post = await getPost(parsedId.data);
  } catch (error) {
    if (error instanceof ApiError && error.code === 'POST_NOT_FOUND') notFound();
    throw error;
  }

  const action = updatePostAction.bind(null, post.id);

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-1.5">
        <h1 className="text-title-md text-ink">게시글 수정</h1>
        <p className="text-caption text-muted">제목과 내용을 고쳐 저장하세요.</p>
      </div>
      <PostForm
        action={action}
        submitLabel="수정"
        initial={{ title: post.title, content: post.content }}
      />
    </div>
  );
}

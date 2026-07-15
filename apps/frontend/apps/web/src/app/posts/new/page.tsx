import type { Metadata } from 'next';
import { createPostAction } from '@/features/board/index.server';
import { PostForm } from '@/features/board/index.client';

export const metadata: Metadata = { title: '새 글 작성' };

export default function NewPostPage() {
  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-1.5">
        <h1 className="text-2xl font-semibold tracking-tight text-ink">새 글 작성</h1>
        <p className="text-sm text-muted">제목과 작성자, 내용을 입력해 글을 남기세요.</p>
      </div>
      <PostForm action={createPostAction} submitLabel="작성" showAuthor />
    </div>
  );
}

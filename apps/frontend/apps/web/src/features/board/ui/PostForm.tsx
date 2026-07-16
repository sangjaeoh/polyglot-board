'use client';

import { useActionState } from 'react';
import Link from 'next/link';
import { TextAreaField, TextField } from '@board/ui';
import type { FormState } from '../model/actions';
import { SubmitButton } from './SubmitButton';

interface PostFormProps {
  action: (prev: FormState | null, formData: FormData) => Promise<FormState>;
  submitLabel: string;
  initial?: { title?: string; content?: string; author?: string };
  showAuthor?: boolean;
}

/** 작성·수정 공용 폼. Server Action 제출이라 useActionState로 필드 에러를 소비한다. */
export function PostForm({ action, submitLabel, initial, showAuthor = false }: PostFormProps) {
  const [state, formAction] = useActionState(action, null);

  return (
    <form action={formAction} className="flex flex-col gap-5">
      <TextField
        name="title"
        label="제목"
        defaultValue={initial?.title}
        maxLength={200}
        required
        error={state?.fieldErrors?.title}
      />
      {showAuthor ? (
        <TextField
          name="author"
          label="작성자"
          defaultValue={initial?.author}
          maxLength={20}
          required
          error={state?.fieldErrors?.author}
        />
      ) : null}
      <TextAreaField
        name="content"
        label="내용"
        defaultValue={initial?.content}
        maxLength={10000}
        required
        error={state?.fieldErrors?.content}
      />
      {state?.message ? (
        <p
          role="alert"
          className="rounded-lg border border-danger-text/25 bg-danger-soft px-3.5 py-3 text-caption text-danger-text"
        >
          {state.message}
        </p>
      ) : null}
      <div className="flex items-center gap-4 pt-1">
        <SubmitButton label={submitLabel} />
        <Link
          href="/"
          className="text-label text-muted transition-colors duration-fast hover:text-ink focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
        >
          취소
        </Link>
      </div>
    </form>
  );
}

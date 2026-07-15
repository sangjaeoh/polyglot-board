'use server';

import { revalidatePath } from 'next/cache';
import { redirect } from 'next/navigation';
import { ApiError, createPost, deletePost, updatePost } from '@board/api-client';
import { postCreateRequestSchema, postUpdateRequestSchema } from 'shared-types';
import type { ZodError } from 'zod';

export interface FormState {
  fieldErrors?: Record<string, string>;
  message?: string;
}

function fieldErrorsFrom(error: ZodError): Record<string, string> {
  const flat = error.flatten().fieldErrors;
  const result: Record<string, string> = {};
  for (const [key, messages] of Object.entries(flat)) {
    const first = messages?.[0];
    if (first) result[key] = first;
  }
  return result;
}

// 백엔드 ProblemDetail(egress 에러)을 폼 상태로 매핑한다. 예상 밖 에러는 에러 바운더리로 넘긴다.
function apiErrorToState(error: unknown): FormState {
  if (error instanceof ApiError) {
    if (error.fieldErrors.length > 0) {
      const fieldErrors: Record<string, string> = {};
      for (const fieldError of error.fieldErrors)
        fieldErrors[fieldError.field] = fieldError.message;
      return { fieldErrors };
    }
    return { message: error.problem.detail ?? error.problem.title };
  }
  throw error;
}

/** 게시글 작성. ingress를 서버에서 다시 엄격 검증한다. */
export async function createPostAction(
  _prev: FormState | null,
  formData: FormData,
): Promise<FormState> {
  const parsed = postCreateRequestSchema.safeParse({
    title: formData.get('title'),
    content: formData.get('content'),
    author: formData.get('author'),
  });
  if (!parsed.success) return { fieldErrors: fieldErrorsFrom(parsed.error) };

  let createdId: string;
  try {
    const created = await createPost(parsed.data);
    createdId = created.id;
  } catch (error) {
    return apiErrorToState(error);
  }
  revalidatePath('/');
  redirect(`/posts/${createdId}`);
}

/** 게시글 수정. id는 bind로 고정한다. */
export async function updatePostAction(
  id: string,
  _prev: FormState | null,
  formData: FormData,
): Promise<FormState> {
  const parsed = postUpdateRequestSchema.safeParse({
    title: formData.get('title'),
    content: formData.get('content'),
  });
  if (!parsed.success) return { fieldErrors: fieldErrorsFrom(parsed.error) };

  try {
    await updatePost(id, parsed.data);
  } catch (error) {
    return apiErrorToState(error);
  }
  revalidatePath('/');
  revalidatePath(`/posts/${id}`);
  redirect(`/posts/${id}`);
}

/** 게시글 삭제. id는 bind로 고정한다. 이미 삭제된 게시글(404)만 성공과 같게 목록으로 진행한다. */
export async function deletePostAction(id: string): Promise<FormState> {
  try {
    await deletePost(id);
  } catch (error) {
    const alreadyDeleted = error instanceof ApiError && error.code === 'POST_NOT_FOUND';
    if (!alreadyDeleted) return apiErrorToState(error);
  }
  revalidatePath('/');
  redirect('/');
}

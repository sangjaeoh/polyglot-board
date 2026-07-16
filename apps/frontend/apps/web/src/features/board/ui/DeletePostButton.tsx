'use client';

import { useActionState, useId, useRef } from 'react';
import { Alert, Button } from '@board/ui';
import { deletePostAction } from '../model/actions';
import { SubmitButton } from './SubmitButton';

/** 파괴적 동작이라 네이티브 dialog로 확인을 거친다 — 포커스 트랩·Escape·포커스 복원이 내장이다. */
export function DeletePostButton({ id }: { id: string }) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  const [state, formAction] = useActionState(deletePostAction.bind(null, id), null);

  return (
    <>
      <Button variant="danger" onClick={() => dialogRef.current?.showModal()}>
        삭제
      </Button>
      <dialog
        ref={dialogRef}
        aria-labelledby={titleId}
        className="m-auto w-full max-w-sm rounded-xl border border-edge bg-surface p-6 text-ink shadow-card-hover backdrop:bg-black/50"
      >
        <form action={formAction} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <h2 id={titleId} className="text-heading text-ink">
              게시글을 삭제할까요?
            </h2>
            <p className="text-caption text-muted">삭제한 게시글은 되돌릴 수 없습니다.</p>
          </div>
          {state?.message ? <Alert>{state.message}</Alert> : null}
          <div className="flex justify-end gap-3 pt-1">
            <Button variant="secondary" onClick={() => dialogRef.current?.close()}>
              취소
            </Button>
            <SubmitButton label="삭제" variant="danger" />
          </div>
        </form>
      </dialog>
    </>
  );
}

'use client';

import { useFormStatus } from 'react-dom';
import { Button } from '@board/ui';

interface SubmitButtonProps {
  label: string;
  variant?: 'primary' | 'danger';
}

export function SubmitButton({ label, variant = 'primary' }: SubmitButtonProps) {
  const { pending } = useFormStatus();
  return (
    <Button type="submit" variant={variant} disabled={pending} aria-busy={pending}>
      {pending ? (
        <>
          <span
            aria-hidden
            className="size-3.5 rounded-full border-2 border-current border-r-transparent motion-safe:animate-spin"
          />
          처리 중…
        </>
      ) : (
        label
      )}
    </Button>
  );
}

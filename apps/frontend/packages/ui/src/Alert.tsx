import { clsx } from 'clsx';
import type { ReactNode } from 'react';

interface AlertProps {
  children: ReactNode;
  className?: string;
}

/** 인라인 위험 경고. 동적으로 나타나는 오류를 assertive live region으로 알린다. 콘텐츠는 슬롯으로 받는다. */
export function Alert({ children, className }: AlertProps) {
  return (
    <p
      role="alert"
      className={clsx(
        'rounded-lg border border-danger-text/25 bg-danger-soft px-3.5 py-3 text-caption text-danger-text',
        className,
      )}
    >
      {children}
    </p>
  );
}

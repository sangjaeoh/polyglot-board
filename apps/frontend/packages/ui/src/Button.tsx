import { clsx } from 'clsx';
import type { ButtonHTMLAttributes } from 'react';

type Variant = 'primary' | 'secondary' | 'danger';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
}

const variantClass: Record<Variant, string> = {
  primary: 'bg-primary text-on-primary shadow-control hover:bg-primary-hover disabled:opacity-50',
  secondary:
    'border border-edge-strong bg-surface text-ink shadow-control hover:bg-surface-hover disabled:opacity-50',
  danger: 'bg-danger text-danger-fg shadow-control hover:bg-danger-hover disabled:opacity-50',
};

export function Button({ variant = 'primary', className, type, ...props }: ButtonProps) {
  return (
    <button
      type={type ?? 'button'}
      className={clsx(
        'inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-medium',
        'transition-[background-color,box-shadow,transform] duration-150 active:translate-y-px',
        'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent',
        'disabled:cursor-not-allowed disabled:active:translate-y-0',
        variantClass[variant],
        className,
      )}
      {...props}
    />
  );
}

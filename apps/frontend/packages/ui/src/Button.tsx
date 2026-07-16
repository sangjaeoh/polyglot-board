import { clsx } from 'clsx';
import type { ButtonHTMLAttributes } from 'react';

export type ButtonVariant = 'primary' | 'secondary' | 'danger';

const variantClass: Record<ButtonVariant, string> = {
  primary: 'bg-primary text-on-primary shadow-control hover:bg-primary-hover disabled:opacity-50',
  secondary:
    'border border-edge-strong bg-surface text-ink shadow-control hover:bg-surface-hover disabled:opacity-50',
  danger: 'bg-danger text-danger-fg shadow-control hover:bg-danger-hover disabled:opacity-50',
};

/**
 * 버튼 시각 스킨(base + variant)을 반환한다. <button>이 아닌 시맨틱 요소(버튼처럼 보이는 링크 등)가
 * 같은 스킨을 소비할 때 쓴다 — 스킨 정본은 여기 한 곳이다. className은 바깥 배치 탈출구다.
 */
export function buttonClass(variant: ButtonVariant = 'primary', className?: string): string {
  return clsx(
    'inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-label',
    'transition-[background-color,box-shadow,transform] duration-fast active:translate-y-px',
    'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent',
    'disabled:cursor-not-allowed disabled:active:translate-y-0',
    variantClass[variant],
    className,
  );
}

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
}

export function Button({ variant = 'primary', className, type, ...props }: ButtonProps) {
  return <button type={type ?? 'button'} className={buttonClass(variant, className)} {...props} />;
}

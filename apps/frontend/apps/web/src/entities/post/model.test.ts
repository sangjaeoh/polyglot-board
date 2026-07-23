import { describe, expect, it } from 'vitest';
import { formatDateTime } from './model';

describe('formatDateTime', () => {
  it('UTC ISO-8601 문자열을 "YYYY-MM-DD HH:mm UTC"로 표시한다', () => {
    expect(formatDateTime('2026-07-23T05:07:00Z')).toBe('2026-07-23 05:07 UTC');
  });

  it('오프셋이 있는 ISO-8601도 UTC로 환산해 표시한다', () => {
    expect(formatDateTime('2026-07-23T14:07:00+09:00')).toBe('2026-07-23 05:07 UTC');
  });
});

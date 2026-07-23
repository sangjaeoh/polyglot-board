import { describe, expect, it } from 'vitest';
import { ApiError } from './error';

describe('ApiError', () => {
  it('code·status·fieldErrors를 ProblemDetail에서 그대로 노출한다', () => {
    const error = new ApiError({
      title: '요청 값이 유효하지 않다.',
      status: 400,
      code: 'VALIDATION_FAILED',
      errors: [{ field: 'title', message: '필수값이다.' }],
    });

    expect(error.code).toBe('VALIDATION_FAILED');
    expect(error.status).toBe(400);
    expect(error.fieldErrors).toEqual([{ field: 'title', message: '필수값이다.' }]);
  });

  it('errors가 없으면 fieldErrors는 빈 배열이다', () => {
    const error = new ApiError({ title: '오류', status: 500, code: 'INTERNAL_ERROR' });

    expect(error.fieldErrors).toEqual([]);
  });
});

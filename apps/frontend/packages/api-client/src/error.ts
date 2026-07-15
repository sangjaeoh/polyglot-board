import type { ProblemDetail } from 'shared-types';

/** 백엔드 ProblemDetail(RFC 9457)을 감싼 타입드 에러. 기계 분기는 {@link code}로만 한다. */
export class ApiError extends Error {
  readonly problem: ProblemDetail;

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title);
    this.name = 'ApiError';
    this.problem = problem;
  }

  get code(): string {
    return this.problem.code;
  }

  get status(): number {
    return this.problem.status;
  }

  get fieldErrors(): ReadonlyArray<{ field: string; message: string }> {
    return this.problem.errors ?? [];
  }
}

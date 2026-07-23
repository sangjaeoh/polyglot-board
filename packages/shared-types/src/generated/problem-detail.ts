// 코드젠 산출물이다. 직접 편집하지 마라.
// 원천: apps/backend/docs/openapi/openapi.json의 components.schemas.ProblemDetail
// 생성: pnpm --filter shared-types codegen (scripts/generate-problem-detail.mjs)
import { z } from "zod";

export const problemDetailSchema = z.object({
  code: z.string(),
  detail: z.string().optional(),
  errors: z
    .array(
      z.object({
        field: z.string(),
        message: z.string(),
      }),
    )
    .optional(),
  instance: z.string().optional(),
  status: z.number(),
  title: z.string(),
  traceId: z.string().optional(),
  type: z.string().optional(),
});

export type ProblemDetail = z.infer<typeof problemDetailSchema>;

#!/usr/bin/env node
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const openapiPath = resolve(__dirname, '../../../apps/backend/docs/openapi/openapi.json');
const outputPath = resolve(__dirname, '../src/generated/problem-detail.ts');

const openapi = JSON.parse(readFileSync(openapiPath, 'utf-8'));
const schema = openapi.components?.schemas?.ProblemDetail;
if (!schema) {
  throw new Error('openapi.json에 components.schemas.ProblemDetail이 없다.');
}

function zodPrimitive(propSchema) {
  switch (propSchema.type) {
    case 'string':
      return 'z.string()';
    case 'integer':
    case 'number':
      return 'z.number()';
    case 'boolean':
      return 'z.boolean()';
    case 'array': {
      const items = propSchema.items;
      if (items?.type === 'object') {
        return `z.array(${zodObject(items)})`;
      }
      throw new Error(`지원하지 않는 배열 항목 타입: ${JSON.stringify(items)}`);
    }
    default:
      throw new Error(`지원하지 않는 스키마 타입: ${JSON.stringify(propSchema)}`);
  }
}

function zodObject(objectSchema) {
  const required = new Set(objectSchema.required ?? []);
  const lines = Object.entries(objectSchema.properties ?? {}).map(([name, propSchema]) => {
    const base = zodPrimitive(propSchema);
    return `  ${name}: ${base}${required.has(name) ? '' : '.optional()'},`;
  });
  return `z.object({\n${lines.join('\n')}\n})`;
}

const body = zodObject(schema);
const output = `// 코드젠 산출물이다. 직접 편집하지 마라.
// 원천: apps/backend/docs/openapi/openapi.json의 components.schemas.ProblemDetail
// 생성: pnpm --filter shared-types codegen (scripts/generate-problem-detail.mjs)
import { z } from 'zod';

export const problemDetailSchema = ${body};

export type ProblemDetail = z.infer<typeof problemDetailSchema>;
`;

mkdirSync(dirname(outputPath), { recursive: true });
writeFileSync(outputPath, output);
console.log(`generated: ${outputPath}`);

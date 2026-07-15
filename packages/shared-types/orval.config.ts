import { defineConfig } from 'orval';

// 계약의 유일 원천 openapi.json에서 Zod 스키마를 생성한다. 타입은 소비처가 z.infer로 파생한다.
// 코드젠 툴은 저위험·가역이다 — seam은 openapi.json이라 다른 툴로 교체해도 소비처는 무영향이다.
export default defineConfig({
  boardZod: {
    input: {
      target: '../../apps/backend/docs/openapi/openapi.json',
    },
    output: {
      mode: 'single',
      client: 'zod',
      target: './src/generated/zod.ts',
      fileExtension: '.ts',
      override: {
        zod: {
          generate: {
            param: true,
            body: true,
            response: true,
            query: true,
            header: false,
          },
        },
      },
    },
  },
});

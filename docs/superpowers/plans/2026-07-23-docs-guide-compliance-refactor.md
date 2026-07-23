# docs 가이드 정합 리팩토링 1차 배치 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 감사 보고서(`docs/superpowers/reports/2026-07-23-docs-compliance-audit.md`)의 우선순위 4건 위반 — backend 상관 ID 미구현, 루트 shared-types 생성물 원칙 위반, frontend 캐시 무효화 정합, frontend 테스트 인프라 부재 — 를 설계 문서(`docs/superpowers/specs/2026-07-23-docs-guide-compliance-refactor-design.md`)대로 고친다.

**Architecture:** 4개 워크스트림(backend/루트+api-client/frontend 테스트/frontend 캐시)을 순차 10개 태스크로 나눈다. B(루트 shared-types)가 D(frontend 테스트)보다 먼저 끝나야 D의 api-client 테스트가 정리된 `schemas.ts`를 대상으로 삼을 수 있다. C(캐시)는 D가 만든 `actions.test.ts`를 이어받아 마지막에 갱신한다.

**Tech Stack:** Spring Boot(Java 25)/Gradle, Next.js 16.2.10(App Router)/TypeScript/Zod/orval, Vitest/Testing Library/MSW.

## Global Constraints

- 모든 backend 코드 변경은 `apps/backend/AGENTS.md`의 작업 원칙(가정 명시, 최소 변경, 목표 주도 실행)을 따른다.
- 백엔드 신규 클래스는 `@NullMarked` package-info를 갖는다(coding-conventions 컨벤션).
- `@RestControllerAdvice`/`@Component`는 `BoardApiApplication`의 `scanBasePackages = "com.board"`로 자동 스캔된다 — 수동 등록 코드를 추가하지 않는다.
- ProblemDetail 응답 필드명은 `traceId`로 고정한다(`docs/sharing.md:119-121`, 계약). 내부 MDC 키 `requestId`와는 별개 — 절대 필드명을 바꾸지 않는다.
- `packages/shared-types/src/index.ts`에는 수기 스키마 정의(브랜드·`.extend()` 조합 등)를 두지 않는다 — 재노출과 생성물 re-export만 허용한다.
- `apps/frontend/packages/api-client`는 `apps/frontend/apps/web`(앱)을 import하지 않는다(packages→apps 금지, dependency-cruiser 규칙).
- `apps/frontend/apps/web/src/entities/post/model.ts`는 클라이언트 번들에도 포함되므로, `@board/api-client`에서 가져오는 것은 **타입 전용**(`import type`)만 허용한다. 값(value) import 금지.
- `revalidateTag(tag, profile)`는 2-인자가 필수 시그니처다(Next 16, 단일 인자 없음). `updateTag(tag)`는 Server Action 내부에서만 호출 가능하다.
- 프론트 테스트 파일은 `{대상}.test.ts(x)`로 대상과 co-located, `__tests__` 디렉터리를 만들지 않는다(`testing.md`).
- 커밋은 태스크 단위로 나눈다. 무관한 파일을 같이 커밋하지 않는다.

---

## Task 1: backend — RequestIdFilter

**Files:**
- Create: `apps/backend/module-common/common-web/src/main/java/com/board/common/web/correlation/RequestIdFilter.java`
- Create: `apps/backend/module-common/common-web/src/main/java/com/board/common/web/correlation/package-info.java`
- Create: `apps/backend/module-common/common-web/src/test/java/com/board/common/web/correlation/RequestIdFilterTest.java`
- Modify: `apps/backend/module-common/common-web/src/main/java/com/board/common/web/error/GlobalExceptionHandler.java:97-111`
- Modify: `apps/backend/module-apps/app-api/src/test/java/com/board/app/api/web/v1/post/BoardControllerIntegrationTest.java`

**Interfaces:**
- Produces: `RequestIdFilter`(빈, 자동 등록) — 응답 헤더 `X-Request-Id`, MDC 키 `requestId`.
- `GlobalExceptionHandler`가 이 MDC 키를 읽어 응답 `traceId` 필드에 싣는다(계약 필드명 불변).

- [ ] **Step 1: RequestIdFilter 단위 테스트 작성**

```java
package com.board.common.web.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    @DisplayName("X-Request-Id 요청 헤더가 있으면 그 값을 응답 헤더에 그대로 반환한다")
    void acceptsIncomingRequestId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "given-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Request-Id")).isEqualTo("given-id");
    }

    @Test
    @DisplayName("X-Request-Id 요청 헤더가 없으면 생성해 응답 헤더에 싣는다")
    void generatesRequestIdWhenMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
    }

    @Test
    @DisplayName("체인 실행 중에는 MDC 키 requestId를 싣고, 완료 후에는 제거한다")
    void loadsAndClearsMdcAroundChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "mdc-check-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] mdcDuringChain = new String[1];
        FilterChain chain = (req, res) -> mdcDuringChain[0] = MDC.get("requestId");

        filter.doFilter(request, response, chain);

        assertThat(mdcDuringChain[0]).isEqualTo("mdc-check-id");
        assertThat(MDC.get("requestId")).isNull();
    }
}
```

- [ ] **Step 2: 테스트 실행해 실패 확인**

Run: `cd apps/backend && ./gradlew :module-common:common-web:test --tests "com.board.common.web.correlation.RequestIdFilterTest"`
Expected: FAIL — `RequestIdFilter` 클래스가 없어 컴파일 실패.

- [ ] **Step 3: package-info 작성**

```java
@NullMarked
package com.board.common.web.correlation;

import org.jspecify.annotations.NullMarked;
```

- [ ] **Step 4: RequestIdFilter 구현**

```java
package com.board.common.web.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 요청 상관 ID를 소유한다 — {@code X-Request-Id} 수용/생성, MDC 적재, 응답 헤더 반환. */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Request-Id";
    private static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader(HEADER_NAME);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        response.setHeader(HEADER_NAME, requestId);
        MDC.put(MDC_KEY, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
```

- [ ] **Step 5: 테스트 실행해 통과 확인**

Run: `cd apps/backend && ./gradlew :module-common:common-web:test --tests "com.board.common.web.correlation.RequestIdFilterTest"`
Expected: PASS (3 tests).

- [ ] **Step 6: GlobalExceptionHandler가 requestId MDC 키를 읽도록 수정**

`apps/backend/module-common/common-web/src/main/java/com/board/common/web/error/GlobalExceptionHandler.java`의 97-111줄을 다음으로 교체:

```java
    /**
     * 관측용 확장 멤버를 싣는다 — {@code instance}는 요청 경로, {@code traceId}는 상관 ID(MDC 키
     * {@code requestId}, {@link RequestIdFilter} 소유. 없으면 생성 UUID). 반환한 traceId를 서버 로그에
     * 남기면 사용자 에러 리포트와 상관시킬 수 있다.
     */
    private static String applyObservability(ProblemDetail problem, @Nullable String path) {
        if (path != null) {
            problem.setInstance(URI.create(path));
        }
        String traceId = MDC.get("requestId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }
        problem.setProperty("traceId", traceId);
        return traceId;
    }
```

같은 파일 상단 import 블록에 `import com.board.common.web.correlation.RequestIdFilter;` 를 `import com.board.common.core.error.BaseException;` 다음 줄에 추가한다(javadoc 참조용).

- [ ] **Step 7: 통합 테스트에 상관 ID 왕복 검증 추가**

`apps/backend/module-apps/app-api/src/test/java/com/board/app/api/web/v1/post/BoardControllerIntegrationTest.java`의 import 블록에 다음을 추가:

```java
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
```

`PaginationSemantics` 중첩 클래스 다음(112줄 `}` 뒤)에 새 중첩 클래스를 추가:

```java

    @Nested
    @DisplayName("상관 ID")
    class CorrelationId {

        @Test
        @DisplayName("X-Request-Id 요청 헤더를 보내면 응답 헤더로 반환하고 에러 응답 traceId와 일치한다")
        void echoesRequestIdAndMatchesTraceId() throws Exception {
            mockMvc.perform(get("/api/v1/posts/{id}", "00000000-0000-0000-0000-000000000000")
                            .header("X-Request-Id", "test-correlation-id"))
                    .andExpect(status().isNotFound())
                    .andExpect(header().string("X-Request-Id", "test-correlation-id"))
                    .andExpect(jsonPath("$.traceId").value("test-correlation-id"));
        }
    }
```

- [ ] **Step 8: 전체 backend 테스트 실행**

Run: `cd apps/backend && ./gradlew :module-common:common-web:test :module-apps:app-api:test`
Expected: PASS 전체.

- [ ] **Step 9: 커밋**

```bash
git add apps/backend/module-common/common-web/src/main/java/com/board/common/web/correlation \
        apps/backend/module-common/common-web/src/test/java/com/board/common/web/correlation \
        apps/backend/module-common/common-web/src/main/java/com/board/common/web/error/GlobalExceptionHandler.java \
        apps/backend/module-apps/app-api/src/test/java/com/board/app/api/web/v1/post/BoardControllerIntegrationTest.java
git commit -m "feat(backend): RequestIdFilter로 요청 상관 ID 구현 (observability.md 정합)"
```

---

## Task 2: 루트 — shared-types ProblemDetail 생성 스크립트 + index.ts 정리

**Files:**
- Create: `packages/shared-types/scripts/generate-problem-detail.mjs`
- Modify: `packages/shared-types/package.json` (`codegen` 스크립트)
- Modify: `packages/shared-types/src/index.ts`

**Interfaces:**
- Produces: `packages/shared-types/src/generated/problem-detail.ts` — `export const problemDetailSchema`, `export type ProblemDetail`. 외부에서 계속 `from 'shared-types'`로 소비(경로 불변).
- Consumes: `apps/backend/docs/openapi/openapi.json`의 `components.schemas.ProblemDetail`.

- [ ] **Step 1: 생성 스크립트 작성**

```js
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
```

- [ ] **Step 2: `codegen` 스크립트에 연결**

`packages/shared-types/package.json`의 `scripts.codegen`을 교체:

```json
    "codegen": "orval --config ./orval.config.ts && node ./scripts/generate-problem-detail.mjs && prettier --write ./src/generated",
```

- [ ] **Step 3: 스크립트 실행해 생성물 확인**

Run: `pnpm --filter shared-types codegen`
Expected: `packages/shared-types/src/generated/problem-detail.ts` 생성. 내용에 `code`·`status`·`title`은 `.optional()` 없이, `detail`·`errors`·`instance`·`traceId`·`type`은 `.optional()`이 붙는지 확인(파일을 열어 확인).

- [ ] **Step 4: index.ts에서 수기 로직 제거**

`packages/shared-types/src/index.ts` 전체를 다음으로 교체:

```ts
import { z } from 'zod';
import {
  getPostResponse,
  listPostsResponse,
  createPostBody,
  updatePostBody,
  listPostsQueryParams,
} from './generated/zod';

// 계약(openapi.json)에서 생성된 Zod를 벤더 중립 이름으로 재노출한다. 타입은 z.infer로 파생(단일 소스).
// create는 201이라 orval이 응답 스키마를 생성하지 않으나 형상이 상세(getPostResponse)와 같아 재사용한다.
// id 브랜드 승격(UUIDv7 타입 정직성, docs/sharing.md)은 프론트 소비 계층(api-client)이 소유한다 —
// 이 패키지는 계약에서 생성된 산출물만 포함한다(docs/architecture.md).
export const postResponseSchema = getPostResponse;
export const postPageResponseSchema = listPostsResponse;
export const postCreateRequestSchema = createPostBody;
export const postUpdateRequestSchema = updatePostBody;
export const postListQuerySchema = listPostsQueryParams;

export type PostResponse = z.infer<typeof postResponseSchema>;
// int64(totalElements)는 계약이 string으로 표현한다(docs/sharing.md 표) — JSON number의 2^53 정밀도
// 손실은 wire에서 발생하므로 소비 측 변환이 아닌 계약 표현이 진짜 해결이다. 생성 스키마가 z.string()이다.
export type PostPageResponse = z.infer<typeof postPageResponseSchema>;
export type PostSummary = PostPageResponse['content'][number];
export type PostCreateRequest = z.infer<typeof postCreateRequestSchema>;
export type PostUpdateRequest = z.infer<typeof postUpdateRequestSchema>;
export type PostListQuery = z.infer<typeof postListQuerySchema>;

export * from './generated/problem-detail';
```

- [ ] **Step 5: 타입체크로 확인**

Run: `pnpm --filter shared-types typecheck`
Expected: PASS. (아직 `packages/shared-types`를 소비하는 `api-client`가 옛 이름을 참조 중이라 typecheck는 이 패키지 자체만 검증한다 — `api-client`쪽 타입 오류는 Task 3에서 해소한다.)

- [ ] **Step 6: 커밋**

```bash
git add packages/shared-types/scripts packages/shared-types/package.json packages/shared-types/src/index.ts packages/shared-types/src/generated/problem-detail.ts
git commit -m "refactor(shared-types): ProblemDetail을 openapi.json에서 생성하고 브랜드 로직을 제거 (architecture.md:56, sharing.md:113 정합)"
```

---

## Task 3: 루트/frontend — api-client가 브랜드 타입을 소유하도록 이전

**Files:**
- Create: `apps/frontend/packages/api-client/src/schemas.ts`
- Modify: `apps/frontend/packages/api-client/src/index.ts`
- Modify: `apps/frontend/packages/api-client/src/client.ts`
- Modify: `apps/frontend/apps/web/src/entities/post/model.ts`
- Modify: `apps/frontend/apps/web/src/entities/post/index.ts`
- Modify: `apps/frontend/apps/web/src/features/board/index.server.ts`
- Modify: `apps/frontend/apps/web/src/features/board/model/actions.ts`

**Interfaces:**
- Produces: `@board/api-client`가 `postIdSchema`, `PostId`, `postResponseSchema`, `postPageResponseSchema`, `PostResponse`, `PostPageResponse`, `PostSummary`를 export.
- Consumes: `packages/shared-types`의 raw `postResponseSchema`/`postPageResponseSchema`(Task 2 산출물, 브랜드 없음).

- [ ] **Step 1: api-client에 schemas.ts 신설**

```ts
import { z } from 'zod';
import {
  postResponseSchema as rawPostResponseSchema,
  postPageResponseSchema as rawPostPageResponseSchema,
} from 'shared-types';

// UUIDv7 id: plain string + 브랜드 타입·format 검증(docs/sharing.md 타입 정직성 — 생성 타입만으로 신뢰하지 않음).
// .uuid()는 형식 검증이며 v7 버전 자체는 검증하지 않는다(버전 보증은 백엔드 생성기 소유).
// 이 브랜드 승격은 프론트 소비 계층(api-client)이 소유한다 — 루트 shared-types는 생성물만 포함한다.
export const postIdSchema = z.string().uuid().brand<'PostId'>();
export type PostId = z.infer<typeof postIdSchema>;

export const postResponseSchema = rawPostResponseSchema.extend({ id: postIdSchema });
export const postPageResponseSchema = rawPostPageResponseSchema.extend({
  content: z
    .array(rawPostPageResponseSchema.shape.content.element.extend({ id: postIdSchema }))
    .describe('페이지 항목 목록'),
});

export type PostResponse = z.infer<typeof postResponseSchema>;
export type PostPageResponse = z.infer<typeof postPageResponseSchema>;
export type PostSummary = PostPageResponse['content'][number];
```

- [ ] **Step 2: api-client index.ts에 재노출 추가**

`apps/frontend/packages/api-client/src/index.ts` 전체를 다음으로 교체:

```ts
export { getPosts, getPost, createPost, updatePost, deletePost } from './client';
export { ApiError } from './error';
export { postIdSchema, postResponseSchema, postPageResponseSchema } from './schemas';
export type { PostId, PostResponse, PostPageResponse, PostSummary } from './schemas';
```

- [ ] **Step 3: client.ts의 import 경로를 schemas.ts로 교체**

`apps/frontend/packages/api-client/src/client.ts` 2-13줄(현재 `import { getServerEnv } ...` 다음 블록)을 다음으로 교체:

```ts
import { getServerEnv } from '@board/config/server';
import { type PostCreateRequest, type PostUpdateRequest, problemDetailSchema } from 'shared-types';
import { postPageResponseSchema, postResponseSchema, type PostId, type PostPageResponse, type PostResponse } from './schemas';
import { z, type ZodType, type ZodTypeDef } from 'zod';
import { ApiError } from './error';
```

나머지 함수 본문(`url`, `jsonHeaders`, `toApiError`, `readValidated`, `readValidatedPage`, `getPosts`, `getPost`, `createPost`, `updatePost`, `deletePost`)은 변경하지 않는다 — import 소스만 바뀐다.

- [ ] **Step 4: api-client 타입체크**

Run: `pnpm --filter @board/api-client typecheck`
Expected: PASS.

- [ ] **Step 5: entities/post/model.ts를 타입 전용 재노출로 전환**

`apps/frontend/apps/web/src/entities/post/model.ts` 전체를 다음으로 교체:

```ts
import type { PostId, PostPageResponse, PostResponse, PostSummary } from '@board/api-client';

export type { PostId, PostResponse, PostSummary, PostPageResponse };

/**
 * ISO-8601 offset 문자열을 UTC 기준 표시 문자열로 바꾼다.
 *
 * <p>로케일·타임존·Date.now 의존을 피해 서버/클라 hydration이 발산하지 않게 결정적으로 포맷한다.
 */
export function formatDateTime(iso: string): string {
  const date = new Date(iso);
  const pad = (n: number): string => String(n).padStart(2, '0');
  return (
    `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())}` +
    ` ${pad(date.getUTCHours())}:${pad(date.getUTCMinutes())} UTC`
  );
}
```

(`postIdSchema`/`postResponseSchema`/`postPageResponseSchema` 값 재노출은 제거한다 — `@board/api-client`는 `server-only`를 포함하므로 클라 번들에도 실리는 이 파일에서 값으로 import하면 poison 전파 위험이 있다. 타입은 컴파일 타임에 지워지므로 안전하다.)

- [ ] **Step 6: entities/post/index.ts 재노출 목록 조정**

`apps/frontend/apps/web/src/entities/post/index.ts` 전체를 다음으로 교체:

```ts
export { formatDateTime, type PostId, type PostResponse, type PostSummary, type PostPageResponse } from './model';
```

- [ ] **Step 7: features/board/index.server.ts가 api-client에서 직접 가져오도록 수정**

`apps/frontend/apps/web/src/features/board/index.server.ts`의 12-13줄(`export type { PostResponse } from '@/entities/post';` / `export { postIdSchema } from '@/entities/post';`)을 다음으로 교체:

```ts
export type { PostResponse } from '@board/api-client';
export { postIdSchema } from '@board/api-client';
```

- [ ] **Step 8: actions.ts import 수정**

`apps/frontend/apps/web/src/features/board/model/actions.ts`의 5-11줄을 다음으로 교체:

```ts
import { ApiError, createPost, deletePost, postIdSchema, updatePost, type PostId } from '@board/api-client';
import { postCreateRequestSchema, postUpdateRequestSchema } from 'shared-types';
```

- [ ] **Step 9: frontend 전체 타입체크**

Run: `cd apps/frontend && pnpm run typecheck` (또는 `pnpm --filter web typecheck`, `pnpm --filter @board/api-client typecheck` 개별 실행)
Expected: PASS. `PostCard.tsx`/`PostList.tsx`는 `@/entities/post`에서 타입만 가져오므로 변경 없이 통과해야 한다.

- [ ] **Step 10: dependency-cruiser로 packages→apps 위반 없는지 확인**

Run: `pnpm run depcruise:frontend`
Expected: PASS. (`api-client`가 여전히 `apps/web`을 import하지 않는지 재확인.)

- [ ] **Step 11: 커밋**

```bash
git add apps/frontend/packages/api-client/src apps/frontend/apps/web/src/entities/post apps/frontend/apps/web/src/features/board/index.server.ts apps/frontend/apps/web/src/features/board/model/actions.ts
git commit -m "refactor(frontend): postId 브랜드 타입을 api-client(소비 계층)로 이전 (sharing.md:70 정합)"
```

---

## Task 4: frontend — api-client 테스트 인프라 + error.ts 테스트

**Files:**
- Modify: `apps/frontend/packages/api-client/package.json`
- Create: `apps/frontend/packages/api-client/vitest.config.ts`
- Create: `apps/frontend/packages/api-client/src/error.test.ts`
- Modify: `turbo.json`

**Interfaces:**
- Produces: `packages/api-client`에서 `pnpm test` 실행 가능. `turbo run test`가 이 패키지를 포함.

- [ ] **Step 1: 의존성 설치**

Run: `pnpm --filter @board/api-client add -D vitest msw`
Expected: `apps/frontend/packages/api-client/package.json`의 `devDependencies`에 `vitest`, `msw` 추가.

- [ ] **Step 2: vitest 설정 작성**

```ts
import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    environment: 'node',
  },
});
```

- [ ] **Step 3: package.json에 test 스크립트 추가**

`apps/frontend/packages/api-client/package.json`의 `scripts`에 다음 줄 추가:

```json
    "test": "vitest run",
```

- [ ] **Step 4: 실패하는 테스트 작성(ApiError)**

```ts
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
```

- [ ] **Step 5: 루트 turbo.json에 test 태스크 등록**

`turbo.json`의 `tasks`에 `"lint"` 블록 뒤(21줄 `},` 다음)에 추가:

```json
    "test": {
      "dependsOn": ["^codegen", "codegen"]
    },
```

`"verify"` 태스크의 `dependsOn` 배열(24줄)을 다음으로 교체:

```json
      "dependsOn": ["lint", "^lint", "typecheck", "^typecheck", "test", "^test", "^codegen", "codegen"]
```

- [ ] **Step 6: 테스트 실행해 통과 확인**

Run: `pnpm --filter @board/api-client test`
Expected: PASS (2 tests).

- [ ] **Step 7: 커밋**

```bash
git add apps/frontend/packages/api-client/package.json apps/frontend/packages/api-client/vitest.config.ts apps/frontend/packages/api-client/src/error.test.ts turbo.json pnpm-lock.yaml
git commit -m "test(frontend): api-client에 Vitest 인프라 도입, turbo test 태스크 등록 (testing.md 정합)"
```

---

## Task 5: frontend — api-client client.ts 테스트(MSW)

**Files:**
- Create: `apps/frontend/packages/api-client/src/client.test.ts`

**Interfaces:**
- Consumes: Task 4의 vitest/msw 인프라, Task 3의 `schemas.ts`(`postIdSchema`).

- [ ] **Step 1: 실패하는 테스트 작성**

```ts
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { createPost, deletePost, getPost, getPosts, updatePost } from './client';
import { postIdSchema } from './schemas';

const BASE_URL = 'http://localhost:8080';
const POST_ID = postIdSchema.parse('01912d68-7b3a-7000-8000-000000000001');

const VALID_POST = {
  id: POST_ID,
  title: '제목',
  content: '본문',
  author: '글쓴이',
  createdAt: '2026-07-23T00:00:00Z',
  updatedAt: '2026-07-23T00:00:00Z',
};

const server = setupServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('getPost', () => {
  it('유효한 상세 응답을 검증해 반환한다', async () => {
    server.use(http.get(`${BASE_URL}/api/v1/posts/:id`, () => HttpResponse.json(VALID_POST)));

    const post = await getPost(POST_ID);

    expect(post.title).toBe('제목');
    expect(post.id).toBe(POST_ID);
  });

  it('404 응답을 ApiError로 변환해 던진다', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/posts/:id`, () =>
        HttpResponse.json(
          { title: '없음', status: 404, code: 'POST_NOT_FOUND' },
          { status: 404 },
        ),
      ),
    );

    await expect(getPost(POST_ID)).rejects.toMatchObject({ code: 'POST_NOT_FOUND' });
  });

  it('egress 검증에 실패하는 응답은 던진다', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    server.use(http.get(`${BASE_URL}/api/v1/posts/:id`, () => HttpResponse.json({ id: POST_ID })));

    await expect(getPost(POST_ID)).rejects.toMatchObject({ code: 'EGRESS_VALIDATION_FAILED' });
  });
});

describe('getPosts', () => {
  it('불량 항목은 드롭하고 정상 항목만 반환한다', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    server.use(
      http.get(`${BASE_URL}/api/v1/posts`, () =>
        HttpResponse.json({
          content: [
            { id: POST_ID, title: '정상', author: '글쓴이', createdAt: '2026-07-23T00:00:00Z' },
            { id: 'not-a-uuid' },
          ],
          page: 1,
          pageSize: 20,
          totalElements: '2',
          totalPages: 1,
        }),
      ),
    );

    const page = await getPosts();

    expect(page.content).toHaveLength(1);
    expect(page.content[0]?.title).toBe('정상');
  });
});

describe('createPost/updatePost/deletePost', () => {
  it('createPost는 생성된 게시글을 반환한다', async () => {
    server.use(http.post(`${BASE_URL}/api/v1/posts`, () => HttpResponse.json(VALID_POST, { status: 201 })));

    const created = await createPost({ title: '제목', content: '본문', author: '글쓴이' });

    expect(created.id).toBe(POST_ID);
  });

  it('updatePost는 수정된 게시글을 반환한다', async () => {
    server.use(http.put(`${BASE_URL}/api/v1/posts/:id`, () => HttpResponse.json(VALID_POST)));

    const updated = await updatePost(POST_ID, { title: '제목', content: '본문' });

    expect(updated.id).toBe(POST_ID);
  });

  it('deletePost는 204에서 정상 완료한다', async () => {
    server.use(http.delete(`${BASE_URL}/api/v1/posts/:id`, () => new HttpResponse(null, { status: 204 })));

    await expect(deletePost(POST_ID)).resolves.toBeUndefined();
  });
});
```

- [ ] **Step 2: 테스트 실행해 통과 확인**

Run: `pnpm --filter @board/api-client test`
Expected: PASS (7 tests). `getPosts`/`deletePost` 응답 바디가 없거나 204인 경로에서 `response.json()`이 예외를 던지면(`deletePost`는 애초에 바디를 읽지 않으므로 문제 없음) 로그만 확인.

- [ ] **Step 3: 커밋**

```bash
git add apps/frontend/packages/api-client/src/client.test.ts
git commit -m "test(frontend): api-client 5개 함수·egress 검증 실패 경로 MSW 테스트"
```

---

## Task 6: frontend — apps/web 테스트 인프라 + entities/post/model 테스트

**Files:**
- Modify: `apps/frontend/apps/web/package.json`
- Create: `apps/frontend/apps/web/vitest.config.ts`
- Create: `apps/frontend/apps/web/vitest.setup.ts`
- Create: `apps/frontend/apps/web/src/entities/post/model.test.ts`

**Interfaces:**
- Produces: `apps/web`에서 `pnpm test` 실행 가능(jsdom 환경).

- [ ] **Step 1: 의존성 설치**

Run: `pnpm --filter web add -D vitest jsdom @testing-library/react @testing-library/jest-dom`

- [ ] **Step 2: vitest 설정 작성**

```ts
import path from 'node:path';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  esbuild: {
    jsx: 'automatic',
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./vitest.setup.ts'],
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
});
```

- [ ] **Step 3: setup 파일 작성**

```ts
import '@testing-library/jest-dom/vitest';
```

- [ ] **Step 4: package.json에 test 스크립트 추가**

`apps/frontend/apps/web/package.json`의 `scripts`에 다음 줄 추가:

```json
    "test": "vitest run",
```

- [ ] **Step 5: 실패하는 테스트 작성(formatDateTime)**

```ts
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
```

- [ ] **Step 6: 테스트 실행해 통과 확인**

Run: `pnpm --filter web test`
Expected: PASS (2 tests).

- [ ] **Step 7: 커밋**

```bash
git add apps/frontend/apps/web/package.json apps/frontend/apps/web/vitest.config.ts apps/frontend/apps/web/vitest.setup.ts apps/frontend/apps/web/src/entities/post/model.test.ts pnpm-lock.yaml
git commit -m "test(frontend): apps/web에 Vitest·Testing Library 인프라 도입"
```

---

## Task 7: frontend — PostCard/PostList 컴포넌트 테스트

**Files:**
- Create: `apps/frontend/apps/web/src/features/board/ui/PostCard.test.tsx`
- Create: `apps/frontend/apps/web/src/features/board/ui/PostList.test.tsx`

**Interfaces:**
- Consumes: Task 6의 vitest/jsdom/Testing Library 인프라.

- [ ] **Step 1: PostCard 실패하는 테스트 작성**

```tsx
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { PostCard } from './PostCard';

vi.mock('next/link', () => ({
  default: ({ href, children, ...props }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...props}>
      {children}
    </a>
  ),
}));

const SUMMARY = {
  id: '01912d68-7b3a-7000-8000-000000000001',
  title: '첫 글',
  author: '글쓴이',
  createdAt: '2026-07-23T05:07:00Z',
};

describe('PostCard', () => {
  it('제목·작성자를 표시하고 상세 링크로 연결한다', () => {
    render(<PostCard post={SUMMARY} />);

    expect(screen.getByText('첫 글')).toBeInTheDocument();
    expect(screen.getByText('글쓴이')).toBeInTheDocument();
    expect(screen.getByRole('link')).toHaveAttribute('href', `/posts/${SUMMARY.id}`);
  });
});
```

- [ ] **Step 2: PostList 실패하는 테스트 작성**

```tsx
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { PostList } from './PostList';

vi.mock('next/link', () => ({
  default: ({ href, children, ...props }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...props}>
      {children}
    </a>
  ),
}));

describe('PostList', () => {
  it('항목이 없으면 빈 상태를 보여준다', () => {
    render(<PostList page={{ content: [], page: 1, pageSize: 20, totalElements: '0', totalPages: 0 }} />);

    expect(screen.getByText('아직 글이 없습니다')).toBeInTheDocument();
  });

  it('항목이 있으면 목록과 페이지 위치를 보여준다', () => {
    render(
      <PostList
        page={{
          content: [
            {
              id: '01912d68-7b3a-7000-8000-000000000001',
              title: '첫 글',
              author: '글쓴이',
              createdAt: '2026-07-23T05:07:00Z',
            },
          ],
          page: 2,
          pageSize: 20,
          totalElements: '21',
          totalPages: 2,
        }}
      />,
    );

    expect(screen.getByText('첫 글')).toBeInTheDocument();
    expect(screen.getByText('2 / 2')).toBeInTheDocument();
  });
});
```

- [ ] **Step 3: 테스트 실행해 통과 확인**

Run: `pnpm --filter web test`
Expected: PASS. `@board/ui`/`@/entities/post` 워크스페이스 패키지 해석에 실패하면(`Cannot find module` 등) `apps/web/vitest.config.ts`의 `resolve.alias`에 해당 패키지를 `src/index.ts`로 직접 매핑을 추가한 뒤 재실행한다.

- [ ] **Step 4: 커밋**

```bash
git add apps/frontend/apps/web/src/features/board/ui/PostCard.test.tsx apps/frontend/apps/web/src/features/board/ui/PostList.test.tsx
git commit -m "test(frontend): PostCard·PostList 렌더 테스트"
```

---

## Task 8: frontend — PostForm 컴포넌트 테스트

**Files:**
- Create: `apps/frontend/apps/web/src/features/board/ui/PostForm.test.tsx`

**Interfaces:**
- Consumes: Task 6의 인프라.

- [ ] **Step 1: 실패하는 테스트 작성**

```tsx
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { PostForm } from './PostForm';
import type { FormState } from '../model/actions';

describe('PostForm', () => {
  it('초기값과 라벨을 채워 렌더한다', () => {
    const action = vi.fn(async (): Promise<FormState> => ({}));

    render(
      <PostForm
        action={action}
        submitLabel="작성"
        initial={{ title: '제목', content: '본문', author: '글쓴이' }}
        showAuthor
      />,
    );

    expect(screen.getByLabelText('제목')).toHaveValue('제목');
    expect(screen.getByLabelText('작성자')).toHaveValue('글쓴이');
    expect(screen.getByLabelText('내용')).toHaveValue('본문');
    expect(screen.getByRole('button', { name: '작성' })).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 테스트 실행해 통과 확인**

Run: `pnpm --filter web test`
Expected: PASS. `getByLabelText`가 라벨 텍스트를 정확히 못 찾으면(`@board/ui`의 `TextField`/`TextAreaField` 실제 접근 가능 이름 확인 후) 셀렉터를 실제 렌더 결과에 맞게 조정한다.

- [ ] **Step 3: 커밋**

```bash
git add apps/frontend/apps/web/src/features/board/ui/PostForm.test.tsx
git commit -m "test(frontend): PostForm 초기 렌더 테스트"
```

---

## Task 9: frontend — Server Action 테스트

**Files:**
- Create: `apps/frontend/apps/web/src/features/board/model/actions.test.ts`

**Interfaces:**
- Consumes: Task 3의 `actions.ts`(현재 `revalidatePath` 사용 — Task 10에서 `updateTag`로 바뀔 예정이며, 이 태스크가 만든 테스트를 Task 10이 갱신한다).

- [ ] **Step 1: 실패하는 테스트 작성**

```ts
// @vitest-environment node
import { redirect } from 'next/navigation';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@board/api-client';

vi.mock('next/navigation', () => ({ redirect: vi.fn() }));
vi.mock('next/cache', () => ({ revalidatePath: vi.fn() }));
vi.mock('@board/api-client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@board/api-client')>();
  return {
    ...actual,
    createPost: vi.fn(),
    updatePost: vi.fn(),
    deletePost: vi.fn(),
  };
});

const { createPost, updatePost, deletePost } = await import('@board/api-client');
const { createPostAction, updatePostAction, deletePostAction } = await import('./actions');

const VALID_POST = {
  id: '01912d68-7b3a-7000-8000-000000000001',
  title: '제목',
  content: '본문',
  author: '글쓴이',
  createdAt: '2026-07-23T00:00:00Z',
  updatedAt: '2026-07-23T00:00:00Z',
};

function formData(fields: Record<string, string>): FormData {
  const data = new FormData();
  for (const [key, value] of Object.entries(fields)) data.set(key, value);
  return data;
}

beforeEach(() => {
  vi.mocked(redirect).mockClear();
});

describe('createPostAction', () => {
  it('입력이 비면 필드 에러를 반환하고 백엔드를 호출하지 않는다', async () => {
    const state = await createPostAction(null, formData({ title: '', content: '', author: '' }));

    expect(state.fieldErrors).toBeDefined();
    expect(createPost).not.toHaveBeenCalled();
  });

  it('작성에 성공하면 상세로 리다이렉트한다', async () => {
    vi.mocked(createPost).mockResolvedValue(VALID_POST as never);

    await createPostAction(null, formData({ title: '제목', content: '본문', author: '글쓴이' }));

    expect(redirect).toHaveBeenCalledWith(`/posts/${VALID_POST.id}`);
  });
});

describe('updatePostAction', () => {
  it('id가 유효하지 않으면 메시지를 반환한다', async () => {
    const state = await updatePostAction('invalid-id', null, formData({ title: '제목', content: '본문' }));

    expect(state.message).toBe('잘못된 요청입니다.');
    expect(updatePost).not.toHaveBeenCalled();
  });
});

describe('deletePostAction', () => {
  it('삭제에 성공하면 목록으로 리다이렉트한다', async () => {
    vi.mocked(deletePost).mockResolvedValue(undefined);

    await deletePostAction(VALID_POST.id);

    expect(redirect).toHaveBeenCalledWith('/');
  });

  it('이미 삭제된 게시글이면 에러를 삼키고 목록으로 리다이렉트한다', async () => {
    vi.mocked(deletePost).mockRejectedValue(new ApiError({ title: '없음', status: 404, code: 'POST_NOT_FOUND' }));

    await deletePostAction(VALID_POST.id);

    expect(redirect).toHaveBeenCalledWith('/');
  });
});
```

- [ ] **Step 2: 테스트 실행해 통과 확인**

Run: `pnpm --filter web test`
Expected: PASS (5 tests). `redirect`가 실제로 예외를 던지지 않는 mock이라 `createPostAction`/`deletePostAction`의 `redirect(...)` 다음에 코드가 없어 안전하다.

- [ ] **Step 3: 커밋**

```bash
git add apps/frontend/apps/web/src/features/board/model/actions.test.ts
git commit -m "test(frontend): Server Action 검증 실패·성공 분기 테스트"
```

---

## Task 10: frontend — 캐시 무효화를 cacheTag/cacheLife/updateTag로 전환

**Files:**
- Create: `apps/frontend/apps/web/src/entities/post/cache.ts`
- Modify: `apps/frontend/apps/web/src/entities/post/index.ts`
- Modify: `apps/frontend/apps/web/next.config.ts`
- Modify: `apps/frontend/apps/web/src/features/board/api/queries.ts`
- Modify: `apps/frontend/apps/web/src/features/board/model/actions.ts`
- Modify: `apps/frontend/apps/web/src/features/board/model/actions.test.ts`

**Interfaces:**
- Produces: `postListTag()`, `postTag(id)` — `apps/web/src/entities/post`가 소유하는 캐시 태그 상수.

- [ ] **Step 1: 캐시 태그 상수 작성**

```ts
export function postListTag(): string {
  return 'post-list';
}

export function postTag(id: string): string {
  return `post-${id}`;
}
```

- [ ] **Step 2: entities/post/index.ts에 재노출 추가**

`apps/frontend/apps/web/src/entities/post/index.ts`을 다음으로 교체:

```ts
export { formatDateTime, type PostId, type PostResponse, type PostSummary, type PostPageResponse } from './model';
export { postListTag, postTag } from './cache';
```

- [ ] **Step 3: cacheComponents 플래그 활성화**

`apps/frontend/apps/web/next.config.ts`의 `nextConfig` 객체에 다음 줄 추가(`output: 'standalone',` 다음):

```ts
  // 'use cache'·cacheTag·cacheLife·updateTag를 쓰기 위한 안정화된 플래그(docs/data.md 캐시 절).
  cacheComponents: true,
```

- [ ] **Step 4: actions.test.ts를 updateTag 기대치로 먼저 갱신(실패 확인용)**

`apps/frontend/apps/web/src/features/board/model/actions.test.ts`의 `vi.mock('next/cache', ...)` 줄을 다음으로 교체:

```ts
vi.mock('next/cache', () => ({ updateTag: vi.fn() }));
```

같은 파일 상단 import 블록의 `import { redirect } from 'next/navigation';` 다음 줄에 추가:

```ts
import { updateTag } from 'next/cache';
```

`createPostAction` "성공하면 상세로 리다이렉트한다" 테스트의 `expect(redirect)...` 줄 앞에 추가:

```ts
    expect(updateTag).toHaveBeenCalledWith('post-list');
```

`deletePostAction` "삭제에 성공하면 목록으로 리다이렉트한다" 테스트의 `expect(redirect)...` 줄 앞에 추가:

```ts
    expect(updateTag).toHaveBeenCalledWith('post-list');
```

- [ ] **Step 5: 테스트 실행해 실패 확인**

Run: `pnpm --filter web test`
Expected: FAIL — `actions.ts`가 아직 `revalidatePath`만 호출하므로 `updateTag`가 호출되지 않았다는 실패, 또는 mock에 `revalidatePath`가 없어 `actions.ts`가 런타임 에러.

- [ ] **Step 6: queries.ts에 'use cache' 적용**

`apps/frontend/apps/web/src/features/board/api/queries.ts` 전체를 다음으로 교체:

```ts
// 서버 전용 세그먼트 자체 가드 — api-client의 전이 가드에 기대지 않는다(리팩터로 조용히 소멸 가능).
import 'server-only';

import { cache } from 'react';
import { cacheLife, cacheTag } from 'next/cache';
import { getPost as fetchPost, getPosts as fetchPosts } from '@board/api-client';
import { postListTag, postTag } from '@/entities/post';
import type { PostId } from '@board/api-client';

// 백엔드 호출은 api-client(server-only)로만 한다. feature가 앱에 노출하는 read 경계다.
async function cachedGetPosts(params: { page?: number; size?: number } = {}) {
  'use cache';
  cacheTag(postListTag());
  cacheLife('minutes');
  return fetchPosts(params);
}

export const getPosts = cachedGetPosts;

// 상세는 페이지·generateMetadata가 함께 호출하므로 요청 단위로도 메모이즈한다.
async function cachedGetPost(id: PostId) {
  'use cache';
  cacheTag(postTag(id));
  cacheLife('minutes');
  return fetchPost(id);
}

export const getPost = cache(cachedGetPost);
```

- [ ] **Step 7: actions.ts의 revalidatePath를 updateTag로 교체**

`apps/frontend/apps/web/src/features/board/model/actions.ts`의 3-4줄(`import { revalidatePath } from 'next/cache'; import { redirect } from 'next/navigation';`)을 다음으로 교체:

```ts
import { updateTag } from 'next/cache';
import { redirect } from 'next/navigation';
```

같은 파일 6줄(`import { ApiError, createPost, deletePost, postIdSchema, updatePost, type PostId } from '@board/api-client';` 다음)에 추가:

```ts
import { postListTag, postTag } from '@/entities/post';
```

`createPostAction`의 `revalidatePath('/');` 줄(62줄)을 다음으로 교체:

```ts
  updateTag(postListTag());
```

`updatePostAction`의 `revalidatePath('/'); revalidatePath(`/posts/${parsedId.data}`);` 두 줄(86-87줄)을 다음으로 교체:

```ts
  updateTag(postListTag());
  updateTag(postTag(parsedId.data));
```

`deletePostAction`의 `revalidatePath('/');` 줄(102줄)을 다음으로 교체:

```ts
  updateTag(postListTag());
```

`revalidateTag(tag, profile)`는 이번 배치의 3개 Server Action 모두 "자기 요청 안에서 즉시 최신 데이터를 봐야 하는" read-your-own-writes 상황이라 `updateTag` 하나로 충분하다(Next.js 공식 문서: `updateTag`는 Server Action 전용, `revalidateTag`는 그 밖의 트리거용). 이 코드베이스엔 Server Action 밖에서 태그를 무효화할 트리거가 아직 없어 `revalidateTag` 호출부는 만들지 않는다(YAGNI).

- [ ] **Step 8: 테스트 실행해 통과 확인**

Run: `pnpm --filter web test`
Expected: PASS 전체.

- [ ] **Step 9: 타입체크**

Run: `pnpm --filter web typecheck`
Expected: PASS.

- [ ] **Step 10: 개발 서버로 골든 패스 수동 확인**

Run: `pnpm --filter web dev`

브라우저로 다음을 확인한다:
1. `/`에서 목록이 로드된다.
2. 새 글 작성 후 상세로 리다이렉트되고, 목록으로 돌아가면 방금 쓴 글이 보인다(캐시가 무효화됐다).
3. 글 수정 후 상세·목록 모두 갱신된 내용이 보인다.
4. 글 삭제 후 목록에서 사라진다.

문제가 있으면(예: `cacheComponents: true`가 다른 라우트에서 Suspense 경계 요구 에러를 낸다면) 해당 라우트에 `<Suspense>` 경계를 추가하거나 원인을 기록해 사용자에게 보고한다 — 이 스텝은 자동 테스트로 대체할 수 없다.

- [ ] **Step 11: 커밋**

```bash
git add apps/frontend/apps/web/src/entities/post apps/frontend/apps/web/next.config.ts apps/frontend/apps/web/src/features/board/api/queries.ts apps/frontend/apps/web/src/features/board/model/actions.ts apps/frontend/apps/web/src/features/board/model/actions.test.ts
git commit -m "refactor(frontend): 캐시 무효화를 cacheTag/cacheLife/updateTag로 전환 (data.md 캐시 절 정합)"
```

---

## 실행 후 전체 검증

모든 태스크 완료 후 한 번에 실행:

```bash
cd apps/backend && ./gradlew build
cd /Users/sangjaeoh/Desktop/gitspace/polyglot-board && pnpm run verify
```

`pnpm run verify`는 `turbo run verify`(lint·typecheck·test) → `depcruise:frontend` → `drift:check` 순서로 전부 실행한다. `drift:check`는 `codegen`을 재실행해 Task 2의 `problem-detail.ts`가 재생성 결과와 커밋본이 일치하는지도 함께 검증한다.

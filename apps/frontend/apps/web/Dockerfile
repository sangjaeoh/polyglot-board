# syntax=docker/dockerfile:1
# 이 파일은 실행 앱(web) 루트에 있으나, 빌드 컨텍스트는 레포 루트다(pnpm 워크스페이스 전체가 필요).
# compose가 context=., dockerfile=apps/frontend/apps/web/Dockerfile로 지정한다. COPY 경로는 컨텍스트(레포 루트) 기준이다.

FROM node:24-slim AS builder
ENV PNPM_HOME=/pnpm
ENV PATH=$PNPM_HOME:$PATH
RUN corepack enable
WORKDIR /repo

# 1) 워크스페이스 매니페스트만 먼저 복사해 의존 설치 레이어를 캐시한다.
COPY pnpm-workspace.yaml package.json turbo.json pnpm-lock.yaml ./
COPY packages/shared-types/package.json ./packages/shared-types/
COPY apps/backend/package.json ./apps/backend/
COPY apps/frontend/apps/web/package.json ./apps/frontend/apps/web/
COPY apps/frontend/packages/config/package.json ./apps/frontend/packages/config/
COPY apps/frontend/packages/ui/package.json ./apps/frontend/packages/ui/
COPY apps/frontend/packages/api-client/package.json ./apps/frontend/packages/api-client/
RUN pnpm install --frozen-lockfile

# 2) 소스 복사 후 web 빌드. 코드젠 산출물(shared-types/src/generated)은 커밋돼 있어 백엔드 없이 빌드된다.
COPY packages ./packages
COPY apps/frontend ./apps/frontend
COPY apps/backend/docs/openapi ./apps/backend/docs/openapi
RUN pnpm --filter web build

# 3) 런타임에는 standalone 산출물만 담는다(dev 의존·소스·전체 .next 제외).
#    output: 'standalone' + outputFileTracingRoot(레포 루트) 설정으로 산출물이 레포 구조를 미러링한다.
#    web에 public/은 없다(폰트는 next/font/local로 번들됨).
FROM node:24-slim AS runner
ENV NODE_ENV=production
WORKDIR /app
COPY --from=builder --chown=node:node /repo/apps/frontend/apps/web/.next/standalone ./
COPY --from=builder --chown=node:node /repo/apps/frontend/apps/web/.next/static ./apps/frontend/apps/web/.next/static

USER node
EXPOSE 3000
ENV PORT=3000
ENV HOSTNAME=0.0.0.0
# 백엔드 호출은 server-only api-client가 컨테이너 네트워크로 한다(BACKEND_API_URL=http://backend:8080).
CMD ["node", "apps/frontend/apps/web/server.js"]

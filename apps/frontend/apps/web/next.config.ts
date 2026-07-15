import path from 'node:path';
import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  // Docker runner 스테이지가 standalone 산출물만 복사하도록 self-contained 서버를 생성한다.
  output: 'standalone',
  // 모노레포에서 file tracing 루트를 레포 루트로 고정한다(추론에 맡기면 워크스페이스 의존이 누락될 수 있다).
  outputFileTracingRoot: path.join(__dirname, '../../../..'),
  // 워크스페이스 TS 패키지(소스 배포)를 Next가 트랜스파일한다.
  transpilePackages: ['@board/ui', '@board/api-client', '@board/config', 'shared-types'],
};

export default nextConfig;

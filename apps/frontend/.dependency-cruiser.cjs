const path = require('node:path');

// dependency-cruiser는 프론트 서브트리 전체(apps/*·packages/*)의 워크스페이스 방향·순환·고아를 강제한다.
// FSD 레이어 방향은 eslint-plugin-boundaries가 소유한다(같은 관심사를 이중 집행하지 않는다).
// 실행은 이 디렉터리를 cwd로 한다: depcruise apps packages --config .dependency-cruiser.cjs
module.exports = {
  forbidden: [
    {
      name: 'no-circular',
      severity: 'error',
      comment: '순환 의존은 빌드·트리셰이킹을 깨뜨린다.',
      from: {},
      to: { circular: true },
    },
    {
      name: 'no-packages-to-apps',
      severity: 'error',
      comment: '의존은 apps→packages 단방향이다. 역방향(packages→apps)을 금지한다.',
      from: { path: '^packages' },
      to: { path: '^apps' },
    },
    {
      name: 'no-cross-app-source',
      severity: 'error',
      comment: '앱 소스는 다른 앱의 소스를 직접 참조하지 않는다(공유 패키지 경유만).',
      from: { path: '^apps/([^/]+)/' },
      to: { path: '^apps/', pathNot: ['^apps/$1/'] },
    },
    {
      name: 'no-package-deep-import',
      severity: 'error',
      comment: '앱은 패키지 public API(package.json exports)로만 import한다. 내부 모듈 딥임포트를 금지한다.',
      from: { path: '^apps' },
      to: {
        path: '^packages/[^/]+/src',
        pathNot: ['^packages/[^/]+/src/index\\.ts$', '^packages/config/src/server\\.ts$'],
      },
    },
    {
      name: 'no-orphans',
      severity: 'error',
      comment: '고아 모듈(어디서도 import 안 됨). 진입 파일·설정 파일은 예외.',
      from: {
        orphan: true,
        pathNot: [
          '(^|/)index\\.(server|client)\\.ts$',
          '\\.d\\.ts$',
          '(^|/)next-env\\.d\\.ts$',
          // Next 라우트·앱 규약 파일은 프레임워크가 관례로 로드하므로 고아가 정상이다.
          '(^|/)(page|layout|loading|error|not-found|global-error|template|default|route|instrumentation)\\.(ts|tsx)$',
          // 패키지 public API(package.json exports 대상)는 소비자가 아직 없어도 정상이다.
          '^packages/[^/]+/src/index\\.ts$',
          '^packages/config/src/server\\.ts$',
          // 도구 설정 파일은 도구가 관례로 로드한다.
          '(^|/)(next|postcss|eslint|vitest)\\.config\\.(js|cjs|mjs|ts)$',
          '(^|/)vitest\\.setup\\.ts$',
          // storybookTest 플러그인이 project.test.include를 강제 대체해 storybook project에
          // 실릴 수 없는 커맨드 회귀 테스트(vitest.config.ts의 browser-commands project)는
          // Vitest가 include 글롭으로 관례 로드하므로 정적 그래프에서 고아가 정상이다.
          '(^|/)vitest\\.[^/]+\\.test\\.ts$',
          // resolve.alias로만 연결되는 테스트 목 파일 — 정적 의존 그래프에 안 잡히는 게 정상이다.
          '(^|/)__mocks__/',
          // 워크벤치 스토리·설정·게이트 스크립트는 Storybook이 관례로 로드한다.
          '\\.stories\\.(ts|tsx)$',
          '(^|/)\\.storybook/',
          '(^|/)scripts/[^/]+\\.mjs$',
        ],
      },
      to: {},
    },
  ],
  options: {
    doNotFollow: { path: 'node_modules' },
    exclude: { path: ['node_modules', '(^|/)\\.next/', '(^|/)storybook-static/'] },
    tsConfig: { fileName: path.join(__dirname, 'apps/web/tsconfig.json') },
    tsPreCompilationDeps: true,
    enhancedResolveOptions: {
      exportsFields: ['exports'],
      conditionNames: ['import', 'require', 'node', 'default'],
    },
  },
};

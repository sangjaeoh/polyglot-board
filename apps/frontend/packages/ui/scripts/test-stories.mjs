// 워크벤치 게이트 오케스트레이션: Storybook 정적 빌드 → 정적 서빙 → test-storybook(스모크+a11y).
// CI 없음 전제 — 로컬 verify가 게이트다(code-quality.md). 정적 빌드는 dev 서버 온디맨드 컴파일보다
// 결정적이라 게이트에 적합하다. 정적 서버는 의존성 최소화를 위해 Node 내장 http로 직접 세운다.
import { spawn } from 'node:child_process';
import { createReadStream, existsSync, statSync } from 'node:fs';
import { createServer } from 'node:http';
import { extname, join, normalize } from 'node:path';
import { fileURLToPath } from 'node:url';

const uiDir = fileURLToPath(new URL('..', import.meta.url));
const bin = (name) => join(uiDir, 'node_modules', '.bin', name);
const staticDir = join(uiDir, 'storybook-static');

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.map': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.woff2': 'font/woff2',
  '.woff': 'font/woff',
  '.ico': 'image/x-icon',
  '.txt': 'text/plain; charset=utf-8',
};

function run(cmd, args, { fatal = true } = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(cmd, args, {
      cwd: uiDir,
      stdio: 'inherit',
      // 게이트는 오프라인·비대화형이다 — 텔레메트리 네트워크·프롬프트를 끈다.
      env: { ...process.env, STORYBOOK_DISABLE_TELEMETRY: '1', STORYBOOK_TELEMETRY_DISABLED: '1' },
    });
    child.on('error', (err) => (fatal ? reject(err) : resolve(1)));
    child.on('exit', (code) => resolve(code ?? 1));
  });
}

function startServer() {
  const server = createServer((req, res) => {
    const urlPath = decodeURIComponent((req.url || '/').split('?')[0]);
    const rel = normalize(urlPath).replace(/^(\.\.[/\\])+/, '');
    let filePath = join(staticDir, rel);
    if (!filePath.startsWith(staticDir)) {
      res.statusCode = 403;
      return res.end('forbidden');
    }
    if (urlPath === '/' || (existsSync(filePath) && statSync(filePath).isDirectory())) {
      filePath = join(staticDir, 'index.html');
    }
    if (!existsSync(filePath)) {
      res.statusCode = 404;
      return res.end('not found');
    }
    res.setHeader('Content-Type', MIME[extname(filePath)] || 'application/octet-stream');
    createReadStream(filePath).pipe(res);
  });
  return new Promise((resolve) => {
    server.listen(0, '127.0.0.1', () => resolve({ server, port: server.address().port }));
  });
}

async function main() {
  // 브라우저 보장(idempotent; 이미 있으면 즉시 통과, 첫 실행만 네트워크). 설치 실패는 비치명 —
  // 오프라인 캐시가 있을 수 있고, 없으면 test-storybook이 명확히 실패한다.
  await run(bin('playwright'), ['install', 'chromium'], { fatal: false });

  const buildCode = await run(bin('storybook'), ['build', '--quiet', '--disable-telemetry']);
  if (buildCode !== 0) process.exit(buildCode);

  const { server, port } = await startServer();
  const testCode = await run(bin('test-storybook'), ['--url', `http://127.0.0.1:${port}`]);
  await new Promise((resolve) => server.close(resolve));
  process.exit(testCode);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});

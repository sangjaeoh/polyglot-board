// 서버 기동 시 env를 1회 검증한다 — 잘못된 설정은 첫 요청이 아니라 부팅 시점에 실패한다.
export async function register(): Promise<void> {
  if (process.env.NEXT_RUNTIME !== 'nodejs') return;
  const [{ getServerEnv }, { getClientEnv }] = await Promise.all([
    import('@board/config/server'),
    import('@board/config'),
  ]);
  try {
    getServerEnv();
    getClientEnv();
  } catch (error) {
    // register의 throw를 Next가 unhandledRejection으로 삼키고 서버를 살려두므로, 직접 종료해야 fail-fast다.
    console.error(error instanceof Error ? error.message : error);
    process.exit(1);
  }
}

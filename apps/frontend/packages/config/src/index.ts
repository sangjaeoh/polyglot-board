// 클라·서버 공용 진입점. 클라이언트에 안전한 값만 노출한다.
// 서버 전용 env 리더는 '@board/config/server'로 분리한다(server-only 가드).
export { APP_NAME, DEFAULT_PAGE_SIZE } from './constants';
export { getClientEnv, type ClientEnv } from './env.client';

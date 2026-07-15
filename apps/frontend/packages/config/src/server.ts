import 'server-only';

// 서버 전용 진입점. 내부 URL·시크릿을 다루는 env 리더를 클라 번들에서 격리한다.
export { getServerEnv, type ServerEnv } from './env';

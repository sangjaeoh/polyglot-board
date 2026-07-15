import type { Metadata, Viewport } from 'next';
import type { ReactNode } from 'react';
import localFont from 'next/font/local';
import Link from 'next/link';
import { APP_NAME, getClientEnv } from '@board/config';
import { RouteProgress } from './_components/RouteProgress';
import './globals.css';

// 폰트는 self-host한다(next/font/local) — 빌드가 네트워크에 의존하지 않아 컨테이너 빌드가 결정적이다.
// 사람이 읽는 본문·제목·작성자를 담는 humanist 산세리프(한글·라틴 동시 커버).
const sansKr = localFont({
  src: [
    { path: './fonts/IBMPlexSansKR-Regular.woff2', weight: '400', style: 'normal' },
    { path: './fonts/IBMPlexSansKR-Medium.woff2', weight: '500', style: 'normal' },
    { path: './fonts/IBMPlexSansKR-SemiBold.woff2', weight: '600', style: 'normal' },
  ],
  variable: '--font-plex-sans-kr',
  display: 'swap',
  // 첫 페인트 크리티컬 경로에서 ~1.31MB 한글 woff2 preload를 뺀다. swap + 한글 폴백으로 tofu 없이 각 weight는 글리프가 처음 그려질 때 lazy-load된다.
  preload: false,
});

// 기계가 만든 데이터(ISO 타임스탬프·개수·페이지 위치)를 담는 모노스페이스.
const mono = localFont({
  src: [{ path: './fonts/IBMPlexMono-Regular.woff2', weight: '400', style: 'normal' }],
  variable: '--font-plex-mono',
  display: 'swap',
});

export const metadata: Metadata = {
  metadataBase: new URL(getClientEnv().NEXT_PUBLIC_SITE_URL),
  title: { default: APP_NAME, template: `%s | ${APP_NAME}` },
  description: '팀의 기록을 남기는 간단한 CRUD 게시판.',
};

export const viewport: Viewport = {
  themeColor: [
    { media: '(prefers-color-scheme: light)', color: '#f6f7f9' },
    { media: '(prefers-color-scheme: dark)', color: '#0c0f13' },
  ],
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="ko" className={`${sansKr.variable} ${mono.variable}`}>
      <body className="min-h-dvh">
        <RouteProgress />
        <header className="sticky top-0 z-10 border-b border-edge bg-canvas/85 backdrop-blur-sm supports-[backdrop-filter]:bg-canvas/70">
          <div className="mx-auto flex max-w-3xl items-center px-6 py-4">
            <Link
              href="/"
              className="group inline-flex items-center gap-2.5 rounded-md focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-accent"
            >
              <span
                aria-hidden
                className="h-5 w-[3px] rounded-full bg-accent transition-transform duration-200 group-hover:scale-y-125"
              />
              <span className="text-base font-semibold tracking-tight text-ink">{APP_NAME}</span>
            </Link>
          </div>
        </header>
        <main className="mx-auto w-full max-w-3xl px-6 py-12 sm:py-16">{children}</main>
      </body>
    </html>
  );
}

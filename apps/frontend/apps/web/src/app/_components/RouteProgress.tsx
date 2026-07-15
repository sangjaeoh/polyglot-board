'use client';

import { useEffect, useRef } from 'react';

/**
 * 라우트 전환 진행 바. 세그먼트 전환(목록↔상세)이 느릴 때만 상단에 얇은 인디케이터를 보인다.
 *
 * <p>전환은 이전 화면을 유지한 채 준비되면 즉시 교체된다(loading 바운더리 없음). 빠른 전환은
 * 바가 뜨기 전에 끝나고, 느린 전환에만 바가 나타난다. 같은 세그먼트(페이지네이션 등)는 이미
 * 즉시 전환되므로 대상에서 제외한다.
 *
 * <p>바 엘리먼트는 항상 렌더하되 기본은 숨김이고, 가시성만 data-active 속성으로 명령형 토글한다.
 * loading 바운더리가 없는 전환은 블로킹 트랜지션이라 그 동안 React가 트리를 이전 상태로 붙잡아
 * state 기반 렌더가 커밋되지 않고, imperative append 노드는 재조정으로 제거될 수 있다. 종료는
 * URL(location.pathname) 변경(=커밋 시점)으로 판정한다 — 커밋 전 낙관적 갱신이 없다.
 */
export function RouteProgress() {
  const barRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let showTimer: ReturnType<typeof setTimeout> | null = null;
    let safetyTimer: ReturnType<typeof setTimeout> | null = null;
    let rafId: number | null = null;

    const hide = () => barRef.current?.removeAttribute('data-active');
    const show = () => barRef.current?.setAttribute('data-active', '');

    const stop = () => {
      if (showTimer) {
        clearTimeout(showTimer);
        showTimer = null;
      }
      if (safetyTimer) {
        clearTimeout(safetyTimer);
        safetyTimer = null;
      }
      if (rafId !== null) {
        cancelAnimationFrame(rafId);
        rafId = null;
      }
      hide();
    };

    const onClick = (event: MouseEvent) => {
      if (event.defaultPrevented || event.button !== 0) return;
      if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
      const anchor = (event.target as Element | null)?.closest?.('a');
      const href = anchor?.getAttribute('href');
      if (!href || !href.startsWith('/')) return;
      const startPath = location.pathname;
      // 같은 세그먼트(searchParam만 바뀌는 페이지네이션 등)는 즉시 전환되므로 바를 띄우지 않는다.
      if (new URL(href, location.origin).pathname === startPath) return;

      stop();
      // 빠른 전환은 이 지연 전에 끝난다. 느린 전환만 바를 노출한다.
      showTimer = setTimeout(show, 180);
      // URL은 전환이 커밋될 때 갱신된다 → 그때 바를 종료한다.
      const waitCommit = () => {
        if (location.pathname !== startPath) {
          stop();
          return;
        }
        rafId = requestAnimationFrame(waitCommit);
      };
      rafId = requestAnimationFrame(waitCommit);
      // 안전장치: 전환이 비정상적으로 오래 걸려도 바를 무한정 두지 않는다.
      safetyTimer = setTimeout(stop, 10000);
    };

    document.addEventListener('click', onClick, true);
    return () => {
      document.removeEventListener('click', onClick, true);
      stop();
    };
  }, []);

  return <div ref={barRef} aria-hidden="true" className="route-progress-bar" />;
}

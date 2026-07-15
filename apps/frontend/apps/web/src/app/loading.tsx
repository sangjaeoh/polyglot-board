import { DEFAULT_PAGE_SIZE } from '@board/config';

// 실제 카드 제목처럼 폭이 들쑥날쑥해 보이도록 index로 순환한다(랜덤은 결정성 위반).
const titleWidths = ['w-2/3', 'w-1/2', 'w-3/4'];

/** 목록 페이지 스켈레톤. 라인 박스 높이를 실제 콘텐츠와 맞춰 교체 시 레이아웃 시프트를 막는다. */
export default function HomeLoading() {
  return (
    <div
      role="status"
      aria-label="게시글 목록을 불러오는 중"
      className="flex animate-pulse flex-col gap-8"
    >
      <div className="flex items-end justify-between gap-4">
        <div className="flex flex-col gap-1.5">
          <div className="flex h-8 items-center">
            <div className="h-6 w-18 rounded-md bg-edge" />
          </div>
          <div className="flex h-4 items-center">
            <div className="h-3 w-14 rounded bg-edge" />
          </div>
        </div>
        <div className="h-10 w-24 shrink-0 rounded-lg bg-edge" />
      </div>
      <div className="flex flex-col gap-6">
        <div className="flex flex-col gap-3">
          {Array.from({ length: DEFAULT_PAGE_SIZE }, (_, index) => (
            <div
              key={index}
              className="flex items-center gap-4 rounded-xl border border-edge bg-surface px-5 py-4 shadow-card"
            >
              <div className="min-w-0 flex-1">
                <div className="flex h-6 items-center">
                  <div
                    className={`h-4 rounded bg-edge ${titleWidths[index % titleWidths.length]}`}
                  />
                </div>
                <div className="mt-1.5 flex h-5 items-center">
                  <div className="h-3.5 w-40 rounded bg-edge" />
                </div>
              </div>
              <div className="h-4 w-4 shrink-0 rounded bg-edge" />
            </div>
          ))}
        </div>
        <div className="flex items-center justify-between">
          <div className="h-9 w-18 rounded-lg bg-edge" />
          <div className="h-4 w-10 rounded bg-edge" />
          <div className="h-9 w-18 rounded-lg bg-edge" />
        </div>
      </div>
    </div>
  );
}

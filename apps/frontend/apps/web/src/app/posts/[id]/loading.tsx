/** 상세 페이지 스켈레톤. 라인 박스 높이를 실제 콘텐츠와 맞춰 교체 시 레이아웃 시프트를 막는다. */
export default function PostDetailLoading() {
  return (
    <div
      role="status"
      aria-label="게시글을 불러오는 중"
      className="flex animate-pulse flex-col gap-8"
    >
      <div className="flex flex-col gap-3 border-l-2 border-accent pl-5">
        <div className="flex h-9 items-center">
          <div className="h-7 w-4/5 rounded-md bg-edge" />
        </div>
        <div className="flex h-5 items-center">
          <div className="h-4 w-44 rounded bg-edge" />
        </div>
      </div>
      <div className="flex max-w-prose flex-col">
        {['w-full', 'w-11/12', 'w-full', 'w-3/5'].map((width, index) => (
          <div key={index} className="flex h-8 items-center">
            <div className={`h-4 rounded bg-edge ${width}`} />
          </div>
        ))}
      </div>
      <div className="flex items-center gap-3 border-t border-edge pt-6">
        <div className="h-10 w-16 rounded-lg bg-edge" />
        <div className="h-10 w-16 rounded-lg bg-edge" />
        <div className="ml-auto flex h-5 items-center">
          <div className="h-4 w-8 rounded bg-edge" />
        </div>
      </div>
    </div>
  );
}

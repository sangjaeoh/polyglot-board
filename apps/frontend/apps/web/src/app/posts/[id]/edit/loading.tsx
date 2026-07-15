/** 수정 페이지 스켈레톤. 입력 상자는 빈 필드 모양 그대로 두고 라벨·버튼만 바로 채운다. */
export default function EditPostLoading() {
  return (
    <div
      role="status"
      aria-label="게시글 수정 폼을 불러오는 중"
      className="flex animate-pulse flex-col gap-8"
    >
      <div className="flex flex-col gap-1.5">
        <div className="flex h-8 items-center">
          <div className="h-6 w-30 rounded-md bg-edge" />
        </div>
        <div className="flex h-5 items-center">
          <div className="h-4 w-52 rounded bg-edge" />
        </div>
      </div>
      <div className="flex flex-col gap-5">
        <div className="flex flex-col gap-1.5">
          <div className="flex h-5 items-center">
            <div className="h-4 w-8 rounded bg-edge" />
          </div>
          <div className="h-10 rounded-lg border border-edge bg-surface shadow-control" />
        </div>
        <div className="flex flex-col gap-1.5">
          <div className="flex h-5 items-center">
            <div className="h-4 w-8 rounded bg-edge" />
          </div>
          <div className="h-44 rounded-lg border border-edge bg-surface shadow-control" />
        </div>
        <div className="flex items-center gap-4 pt-1">
          <div className="h-10 w-16 rounded-lg bg-edge" />
          <div className="flex h-5 items-center">
            <div className="h-4 w-8 rounded bg-edge" />
          </div>
        </div>
      </div>
    </div>
  );
}

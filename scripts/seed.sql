-- 데모 시드. docker compose의 seed 서비스가 백엔드 기동(Flyway 완료) 후 실행한다.
-- 보드가 비어 있을 때만 넣어 재실행에도 중복되지 않는다. created_at은 now() 기준 staggered(최신순).
-- id는 고정 UUIDv7 리터럴이다(엔티티 규칙). PostgreSQL 17에는 네이티브 uuidv7()이 없어
-- 리터럴로 넣고, 시간 성분(2026-07-15 UTC 기준 동일 간격)은 created_at 순서와 정합한다.
-- 모든 글은 운영자(안내·소개)로 통일한다.
insert into board.post (id, title, content, author, created_at, updated_at, deleted_at)
select t.id::uuid, t.title, t.content, '운영자', t.created_at, t.created_at, null
from (
    values
        (
            '019f6305-0060-7af4-be5c-51288078f622',
            '게시판 데모에 오신 것을 환영합니다',
            $$이 저장소를 클론해 docker compose up 한 번으로 실행되는 게시판 데모입니다.

위의 '새 글 작성'으로 첫 글을 남겨 보고, 목록에서 글을 열어 수정과 삭제도 눌러 보세요. 목록·상세·작성·수정·삭제가 모두 동작합니다.$$,
            now() - interval '15 minutes'
        ),
        (
            '019f62db-cd80-7b55-bab9-627daaf23b63',
            '이 저장소는 무엇을 확인하려는 곳인가요',
            $$폴리글랏 모노레포 — 서로 다른 언어(여기서는 Java/Spring Boot와 TypeScript/Next.js)가 한 저장소에서 함께 빌드되고 하나의 계약(OpenAPI)으로 이어지는 구조 — 가 실제로 동작하는지 연습하고 확인하려고 만든 데모입니다.

이 게시판이 목록·작성·수정·삭제까지 끝까지 도는 것 자체가, 백엔드와 프론트엔드가 계약으로 제대로 연결됐다는 증거예요.$$,
            now() - interval '1 hour'
        ),
        (
            '019f626d-f080-71cb-8806-d60640d258db',
            '무엇을 해볼 수 있나요',
            $$글 목록(최신순), 글 상세 보기, 글 작성, 수정, 삭제(소프트 삭제)를 지원합니다.

제목은 200자, 내용은 10,000자까지 입력할 수 있어요. 부담 없이 이것저것 시도해 보세요.$$,
            now() - interval '3 hours'
        ),
        (
            '019f615b-4800-79c3-9d3b-1133125e4bd7',
            '다크 모드를 지원합니다',
            $$운영체제의 테마 설정에 따라 라이트/다크 화면이 자동으로 바뀝니다. 설정을 바꿔 두 모드를 모두 확인해 보세요.$$,
            now() - interval '8 hours'
        ),
        (
            '019f5dec-6000-7953-86c4-f5453eaa73a5',
            '삭제한 글은 어떻게 되나요',
            $$글을 삭제해도 데이터가 완전히 지워지지 않고 목록에서만 사라집니다(소프트 삭제). 그래서 목록에는 활성 글만 최신순으로 표시돼요.$$,
            now() - interval '1 day'
        ),
        (
            '019f58c6-0400-715c-aea4-f5f7103460e4',
            '처음 상태로 되돌리려면',
            $$이것저것 시도하다 정리하고 싶어지면 docker compose down -v 로 볼륨을 지운 뒤 다시 up 하세요. 예시 글이 담긴 깔끔한 상태로 초기화됩니다.$$,
            now() - interval '2 days'
        ),
        (
            '019f539f-a800-79b0-a220-19fe7ccded4b',
            '이 예제는 어떻게 만들었나요',
            $$Spring Boot 백엔드와 Next.js 프론트엔드를 계약(OpenAPI)으로 이은 최소 CRUD 예제입니다. 복잡한 기능 없이 '게시판이 돌아간다'를 확인하는 데 초점을 맞췄어요.$$,
            now() - interval '3 days'
        )
) as t (id, title, content, created_at)
where not exists (select 1 from board.post);

-- board 스키마는 SchemaFlyway가 createSchemas로 생성한다. 여기선 defaultSchema(board) 안에 테이블을 만든다.
create table post (
    id         uuid                     primary key,
    title      varchar(200)             not null,
    content    varchar(10000)           not null,
    author     varchar(20)              not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    deleted_at timestamp with time zone
);

-- 활성-only 최신순 목록 조회용 부분 인덱스(물리 FK 없음 → 인덱스는 마이그레이션이 소유).
create index idx_post_active_latest
    on post (created_at desc, id desc)
    where deleted_at is null;

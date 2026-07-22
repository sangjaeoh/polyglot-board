# 게시판 도메인 모델 (Domain Model)

이 문서는 게시판 최소 구현의 도메인 모델을 소유한다 — 어떤 도메인이 있고, 각 도메인이 어떤 엔티티·필드·정책·불변식·오퍼레이션을 갖는지. 즉 "실제로 무엇을 만드는가"를 필드 수준까지 고정한다.

- 요구사항(*무엇을*)은 [`REQUIREMENTS.md`](./REQUIREMENTS.md)가 소유한다. 이 문서는 그 요구사항을 실현하는 모델을 소유한다.
- 설계 규칙(ID·연관·소프트삭제 등)은 `docs/architecture.md`·`docs/entity-persistence.md`·`docs/coding-conventions.md`를 따른다. 이 문서는 그 규칙을 이 앱의 board 도메인에 적용한 결과다.
- 모듈 구조·빌드 순서 같은 "어떻게 세우는가"는 이 문서의 범위가 아니다(아키텍처 규칙 문서 `docs/`가 소유).

범위는 1개 도메인: 게시판(board).

## 공통 규약

- 식별자: 모든 엔티티 PK는 앱에서 생성하는 UUIDv7. `@GeneratedValue` 없음.
- 시각: `createdAt`·`updatedAt`은 JPA Auditing이 채운다(엔티티가 직접 선언하지 않음).
- 삭제: 논리삭제 기본. 삭제 지원 엔티티는 nullable `deletedAt`을 두고 활성 조회에서 제외한다.
- 물리 FK 없음. 인덱스는 마이그레이션 SQL이 소유한다.
- 표기: 아래 필드 표는 이 공통 필드도 함께 명시한다 — 모든 엔티티는 `id`·`createdAt`·`updatedAt`을 가지며, 해당 도메인에 한해 `deletedAt`을 갖는다.

## 1. 게시판 (board)

게시글의 생성·조회·수정·소프트삭제를 소유한다.

- 애그리거트 루트: `Post`
- 스키마/테이블: `board` / `post`

### 필드

| 필드 | 타입 | 필수 | 제약·설명 |
|---|---|---|---|
| id | UUID | 필수 | PK, UUIDv7 |
| title | String | 필수 | 제목. 최대 200자 |
| content | String | 필수 | 본문. 최대 10000자 |
| author | String | 필수 | 작성자 표시 문자열. 최대 20자. 생성 후 불변(수정 오퍼레이션 없음) |
| createdAt | Instant | 필수 | 생성 시각(Auditing 자동 기록) |
| updatedAt | Instant | 필수 | 수정 시각(Auditing 자동 기록) |
| deletedAt | Instant | 선택 | 논리삭제 시각 |

- 길이 제약(200·10000·20)과 공백 거부는 경계(Bean Validation)와 DB 컬럼이 강제하고, 도메인은 non-null만 강제한다(경계 검증 이후의 선행조건 백스톱).

### 정책·불변식

- 생성(create): 필수값은 title, content, author. null이면 거부한다. ID는 앱이 UUIDv7로 생성한다.
- 수정(edit): title·content를 교체한다. null이면 거부한다. author는 불변이다.
- 소프트삭제(delete): `deletedAt` 세팅(논리삭제). 물리 DELETE 하지 않는다.
- 활성 조회: 모든 조회·수정·삭제는 활성 게시글(`deletedAt IS NULL`)만 대상이다. 없거나 삭제된 게시글은 `POST_NOT_FOUND` 예외로 거부한다.
- 목록 정렬: 최신순 — 생성시각 내림차순, 동시각은 id 내림차순. 활성-only 최신순 부분 인덱스가 받친다.

### 오퍼레이션

| 연산 | 입력 | 강제 불변식 | 거부 |
|---|---|---|---|
| register | title, content, author | 필수값 non-null, UUIDv7 ID 앱 생성 | 필수값 누락 |
| getPost | postId | 활성 게시글 1행 | 미존재·삭제됨(POST_NOT_FOUND) |
| getPosts | page, size | 활성-only, 최신순(생성시각·id 내림차순) 페이지 | — |
| edit | postId, title, content | 활성 게시글만, title·content 교체, author 불변 | 미존재·삭제됨(POST_NOT_FOUND); 필수값 누락 |
| remove | postId | 활성 게시글만, `deletedAt` 세팅(논리삭제) | 미존재·삭제됨(POST_NOT_FOUND) |

반환 형상(명령/조회)·거부의 예외 매핑은 `docs/coding-conventions.md`가 소유. 서비스 역할 배치·네이밍도 같은 문서가 소유한다.

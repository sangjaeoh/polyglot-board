# WORKPLAN — backend 코드를 개정 규칙 5문서에 정렬

## 전제 (합의된 판정)

- 단일 애플리케이션 전제: 현존 모듈 세트(module-common 3종·domain-board·app-api·app-migration·test-architecture)는 유지한다. 신규 모듈(events·query·infra·domain-shared·app-admin·app-batch·common-event·common-auth) 신설은 보류한다.
- 적용: 3구역 패키지 구조, provided/required 노출 규칙, 경계 원칙, 트랜잭션 경계, 리포지토리 접근 범위, coding-conventions·entity-persistence·testing·code-quality 전반, 현존 유형의 컨벤션 플러그인·아키텍처 테스트 정렬.
- 질의 합의: 용어집은 backend 루트의 요구사항·도메인 모델 문서 신설로 해결한다(형식은 spring-boot-commerce의 `REQUIREMENTS.md`·`DOMAIN_MODEL.md`를 따른다). 인증·인가 표면은 실물 없음으로 보류한다(a).
- 이 작업은 문서 기준 리팩터다. 새 행동을 만들지 않으며, 기존 행동의 시나리오 보강 테스트는 즉시 통과가 정상이다.

## 갭 분석 (적용 판정 규칙군 대비 현행)

| # | 갭 | 위반 규칙 |
|---|---|---|
| G1 | domain-board가 평탄 패키지(`entity·repository·service·info·exception`)이고 provided 계약 인터페이스가 없다. 서비스가 public 구체 클래스로 앱에 직접 주입된다 | architecture: 도메인 모듈 구조(3구역)·노출 규칙·경계 원칙(진입 계약) |
| G2 | ArchitectureTest의 도메인 루트 파생 마커가 구 패키지(entity·info·…)에 결합. 구역 의존 방향·provided 경유·JPA 타입 시그니처 비노출 등 개정 불변식 규칙 부재 | architecture: 빌드가 강제하는 불변식 |
| G3 | 컨벤션 플러그인 화이트리스트가 유령 모듈(`common-messaging`·`module-external`)을 참조하고 개정 표와 불일치 | architecture: 컨벤션 플러그인 |
| G4 | app-migration이 domain-board를 `implementation` 의존한다 | architecture: app-migration(도메인 모듈은 런타임 전용 의존) |
| G5 | `PostAppender.register`가 PostInfo를 반환한다(명령은 ID 등 최소 결과). `BoardFacade.update`·`BoardController.updatePost`가 범용 동사 `update`를 쓴다 | architecture: 경계 원칙(명령 결과) / coding-conventions: 메서드 네이밍 |
| G6 | 요구사항·도메인 모델 문서(용어집 겸)가 없다 | coding-conventions: 네이밍·주석(용어집 참조) |
| G7 | 리포지토리 테스트가 `@SpringBootTest`(영속 슬라이스 아님). 서비스 테스트 3종이 리포지토리를 Mockito 목으로 대체(목 정책 위반). Appender 테스트 부재. 웹 슬라이스(`@WebMvcTest`) 부재 — 에러 매핑·검증 경계가 전부 통합 레벨에 있음. Bean Validation 단위 테스트 부재. `@DisplayName` 전무 | testing: 테스트 레벨·목 정책·배치·네이밍·시나리오 충분성 |

갭 아님(이미 정합): 앱 구성(OSIV off·ddl-auto validate·migration 분리·JPA 오토컨피그 제외), 엔티티 골격(BaseTimeEntity·UUIDv7·소프트삭제·물리 FK 없음), 페이징(1-based 요청/응답·0-based 도메인), OpenAPI 전역 선언·drift 게이트, 품질 게이트(Spotless·NullAway·Error Prone·Lombok/H2 차단), 마이그레이션 SQL 소유·부분 인덱스.

## 태스크

각 태스크는 완료 시 `./gradlew build` 통과를 확인하고 개별 커밋한다(push 금지). 브랜치: `feat/rules-alignment-v2`.

### T1. 요구사항·도메인 모델 문서 신설 — G6

backend 루트(`apps/backend/`)에 두 문서를 신설한다. 형식·구조는 spring-boot-commerce의 동명 문서를 따르되, 내용은 현행 board 구현의 as-is를 역산해 고정한다(새 요구·기능을 발명하지 않는다).

- `REQUIREMENTS.md` — 무엇을 만드는가.
  - 개요·대상 사용자(익명 사용자 — 인증 없음), 기준 방향(단일 도메인 모놀리스).
  - 범위: 포함(게시글 작성·상세·최신순 페이지 목록·수정·소프트삭제) / 제외(인증·인가, 댓글, 검색, 작성자 소유권 검사 — 보류 판정과 정합).
  - 용어 표: 게시글(Post)·제목·본문·작성자·소프트삭제·활성 게시글 등 — 이 표가 도메인 용어집을 겸한다.
  - 기능·비기능 요구사항: 현행 행동(1-based 페이지네이션, 소프트삭제 기본 제외 조회, ProblemDetail 에러 계약, OpenAPI 계약 원천), 제약·전제(작성자는 자유 문자열 스냅샷, 단일 DB·단일 스키마).
- `DOMAIN_MODEL.md` — 실제로 무엇을 만드는가(필드 수준).
  - 공통 규약: UUIDv7 앱 생성 PK·Auditing 시각·논리삭제·물리 FK 없음(규칙 문서 참조 형식은 커머스 문서와 동일 — 규칙 본문 재서술 금지).
  - board 도메인: `Post` 애그리거트 필드 표(id·title·content·author·createdAt·updatedAt·deletedAt, 길이 제약), 스키마/테이블(`board`/`post`), 정책·불변식(생성 필수값·수정 범위·소프트삭제), 오퍼레이션 표(register·getPost·getPosts·edit·remove — 입력·강제 불변식·거부).
- `coding-conventions.md`의 "도메인 용어집은 `docs/`의 단일 문서가 소유한다"를 새 정본 위치(backend 루트 `REQUIREMENTS.md` 용어 표·`DOMAIN_MODEL.md`)로 갱신한다.
- `AGENTS.md` 앵커에 두 문서 참조를 추가한다(편집 규약 준수).
- 검증: 문서 리뷰 — 필드 표·오퍼레이션 표가 현행 코드와 1:1로 일치하는지 대조(빌드 영향 없음).

### T2. 빌드 배선 정렬 — G3·G4

- `convention.domain-module`: 허용 화이트리스트를 개정 표로 정렬 — `:module-domains:domain-shared`·`:module-common:`(event 모듈은 생성 시 등록). `common-messaging` 참조 제거, 주석 갱신.
- `convention.app-module`: `:module-external:` 제거, 개정 표(`:module-domains:`·`:module-infra:`·`:module-common:`·`:module-events:`·`:module-query:`)로 정렬.
- `app-migration/build.gradle.kts`: domain-board 의존을 `runtimeOnly`로 전환(마이그레이션 리소스 취득 목적).
- 검증: `./gradlew build`.

### T3. 도메인 모듈 3구역 재편 + 아키텍처 테스트 정렬 — G1·G2 (원자 커밋)

패키지 이동:

| 현행 | 개정 |
|---|---|
| `com.board.board.entity.Post` | `com.board.board.domain.Post` |
| `com.board.board.exception.*` | `com.board.board.domain.exception.*` |
| `com.board.board.repository.PostRepository` | `com.board.board.application.required.PostRepository` |
| `com.board.board.info.PostInfo` | `com.board.board.application.info.PostInfo` |
| `com.board.board.service.*` | `com.board.board.application.*` |

provided 계약 도입:

- `application/provided`에 인터페이스 4종: `PostReader`·`PostAppender`·`PostModifier`·`PostRemover`(역할 접미사 이름은 계약이 소유).
- 구현은 `application`에 평탄 배치, package-private `DefaultPostReader` 등으로 개명. `@Transactional`은 구현이 유지.
- `adapter` 구역은 실물 계약이 없으므로 만들지 않는다(하위 패키지는 실물 구현이 생길 때).
- `BoardFacade`는 provided 인터페이스를 주입받는다. app-api·테스트의 임포트 갱신.
- 신설 패키지마다 `package-info.java`(`@NullMarked`) 추가.

아키텍처 테스트 정렬(같은 커밋 — 루트 파생이 패키지 마커에 결합):

- 도메인 루트 파생 마커를 3구역(`application`·`adapter`·`domain`)으로 교체.
- 갱신: 앱의 리포지토리 접근 금지(`.application.required..` 기준), 엔티티 비노출을 패키지 기반에서 JPA 매핑 애노테이션 기반으로 재작성(domain 구역의 비-JPA 타입은 노출 가능이므로).
- 신설 규칙: 구역 의존 방향(`domain`은 타 구역 비의존, `application`은 `adapter` 비의존), `adapter`의 모듈 밖 비노출, 모듈 밖의 `application` 의존은 `provided`·`info`만 허용, provided·info 시그니처에 JPA 매핑 타입 금지, provided 구현 서비스의 package-private 확인.
- 검증: `./gradlew build` (기존 행동 테스트 전부 통과 — 리팩터 기준).

### T4. 명령 최소 반환·범용 동사 정렬 — G5

- provided `PostAppender.register`는 `UUID`(ID)만 반환한다. `BoardController.createPost`는 등록 후 `getPost` 재조회로 201 응답을 조립한다(수정 핸들러와 동일 패턴).
  - 트레이드오프: 작성 요청당 SELECT 1회 추가. 문서가 정본("명령은 ID 등 최소 결과만 반환")이므로 수용한다.
- `BoardFacade.update` → `edit`, `BoardController.updatePost` → `editPost` 개명(범용 동사 금지).
- 응답 계약(형상·상태)은 불변 — openapi.json drift 게이트로 확인.
- 검증: `./gradlew build`.

### T5. 테스트 정렬 — G7

레벨 재배치와 시나리오 보강. 아래 시나리오 목록이 합의 대상이며, 합의 문장은 `@DisplayName`과 1:1 대응한다. 전 테스트에 `@Nested` 그룹·준비/실행/검증 3단(빈 줄)·영어 camelCase 메서드명을 적용한다.

단위 — `Post` (기존 재정비):
1. 게시글을 생성하면 ID가 부여되고 제목·본문·작성자가 설정된다
2. 제목·본문·작성자가 null이면 생성이 거부된다 (`@ParameterizedTest`)
3. 게시글을 수정하면 제목·본문이 교체되고 작성자는 유지된다
4. 수정할 제목·본문이 null이면 수정이 거부된다
5. 게시글을 삭제하면 삭제 시각이 설정된다

단위 — Bean Validation·페이징 (신설, standalone `Validator`; 유한 계약 전수 — 제약마다 위반 값 하나, 경계값과 경계 밖 첫 값):
6. 게시글 작성 요청은 빈 제목·200자 초과 제목·빈 본문·10000자 초과 본문·빈 작성자·20자 초과 작성자를 각각 거부한다
7. 게시글 수정 요청은 빈 제목·200자 초과 제목·빈 본문·10000자 초과 본문을 각각 거부한다
8. 페이징 요청은 page 1 미만·size 1 미만·size 100 초과를 거부하고 경계값(page 1·size 1·size 100)을 수용한다
9. 페이징 요청은 파라미터 생략 시 page 1·size 20으로 보정하고, 1-based 페이지를 0-based로 변환한다 (common-web)
10. 페이지 응답은 0-based Page를 1-based page로 보정하고 전체 항목 수를 문자열로 싣는다 (common-web)

영속 슬라이스 — 리포지토리 (`@SpringBootTest` → `@DataJpaTest` 전환):
11. 활성 게시글 id 조회는 소프트삭제된 게시글을 제외한다
12. 활성 게시글 목록 조회는 소프트삭제분을 제외하고 최신순으로 정렬한다

영속 슬라이스 — 도메인 서비스 (목 제거, `@DataJpaTest` + `@Import` 실 DB 전환; 수정 행동은 flush 후 재조회로 검증):
13. 게시글을 등록하면 영속되고 ID가 반환된다 (신설)
14. 활성 게시글을 조회하면 Info로 반환된다 / 없거나 삭제된 게시글 조회는 POST_NOT_FOUND 예외를 던진다
15. 활성 게시글을 수정하면 영속 상태에 반영된다 / 없거나 삭제된 게시글 수정은 POST_NOT_FOUND 예외를 던진다
16. 활성 게시글을 삭제하면 삭제 시각이 영속된다 / 없거나 삭제된 게시글 삭제는 POST_NOT_FOUND 예외를 던진다

웹 슬라이스 — `BoardController` (신설, `@WebMvcTest` + 파사드 `@MockitoBean`; 인가 게이트는 보류 판정에 따라 제외):
17. 존재하지 않는 게시글 조회는 404와 POST_NOT_FOUND 코드로 응답한다 (에러 경로 축 — 유일 ErrorCode 전수)
18. UUID가 아닌 경로 변수는 400으로 응답한다
19. 잘못된 JSON 본문은 400으로 응답한다
20. 검증 위반 요청 본문은 400 VALIDATION_FAILED와 필드 오류 목록으로 응답한다 (`@Valid` 배선 대표 — 작성 요청)
21. 범위 밖 페이징 파라미터는 400 VALIDATION_FAILED로 응답한다 (`@ParameterObject` 배선 대표)

통합 — 대표 경로 소수로 축소:
22. 게시글 작성→상세→목록→수정→삭제→404의 대표 경로가 동작한다 (crudFlow 유지)
23. size=1로 두 페이지를 차례로 조회하면 최신순 오프셋이 유지된다 (1-based 의미론 핀 유지)
- 삭제: invalidUuidPath·malformedJson·outOfRangePageParams·boundaryPageParams·pageOmittedDefaults·validationFailure (18~21·8~9로 이관 — 같은 행동을 상위 레벨에서 반복하지 않는다)

배선: common-web에 테스트 의존(starter-test·launcher) 추가. OpenApiSnapshotTest·MigrationApplicationTest는 유지.

- 검증: `./gradlew build`, 시나리오 문장 ↔ `@DisplayName` 1:1 확인.

### T6. 최종 자기검증

- architecture.md "빌드가 강제하는 불변식" 목록 전 항목 대조(적용 판정 행 한정).
- testing.md "완료 판정" 체크리스트 대조.
- openapi.json drift 게이트 통과 확인(계약 불변).
- `./gradlew build` 전체 통과. WORKPLAN.md 제거 후 최종 커밋.

## 중지·질의 규칙

- 문서에 답이 없는 판정이 나오면 멈추고 질의한다.
- 시나리오 목록(T5)의 축소·삭제는 재합의 없이 하지 않는다.

## 작업 로그

- T1 완료 — `REQUIREMENTS.md`·`DOMAIN_MODEL.md` 신설(as-is 역산, 커머스 문서 형식), coding-conventions 용어집 소유를 backend 루트 두 문서로 갱신, AGENTS.md 앵커에 참조 추가. 반환 형상은 coding-conventions 소유로 참조만 해 T4 변경과 충돌 없음. 커밋: 4130113
- T2 완료 — domain-module 화이트리스트를 `domain-shared`·`:module-common:`으로 정렬(common-messaging 유령 참조 제거), app-module에서 `:module-external:` 제거·`:module-events:`·`:module-query:` 추가, app-migration의 domain-board를 `runtimeOnly` 전환(마이그레이션 테스트 통과 확인). 커밋: (본 커밋 — 다음 태스크에서 해시 기입)

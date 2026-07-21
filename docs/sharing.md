# Sharing

## 언제

- 백엔드·프론트 간 계약을 생성하거나 변경할 때.
- 코드젠·drift·에러 모델·타입 무결성을 검증할 때.
- 공유 계약·생성 타입을 추가할 때.

## 규칙

### 계약 seam

- 계약 단위는 HTTP를 제공하는 백엔드 실행 단위다.
- 방출 단위당 계약 하나, 계약 문서 하나를 둔다.
  - 방출 단위 식별·구성은 백엔드 서브가이드가 소유한다.
  - 루트는 방출 단위와 계약 파일의 1:1 대응만 강제한다.
  - HTTP 표면이 없는 batch·worker는 계약을 방출하지 않고 seam에 참여하지 않는다.
  - 여러 방출 단위 계약을 하나의 문서로 병합하지 않는다.
    - operationId·스키마 이름 충돌과 drift 원인 추적 불가능을 방지한다.
- 계약의 유일 언어중립 원천은 방출 단위가 코드퍼스트로 방출하는 OpenAPI 문서다.
  - springdoc·키 정렬·스냅샷 테스트를 사용한다.
- 계약 파일은 `apps/backend/docs/openapi/`에 둔다.
  - 파일명은 방출 단위명을 사용한다.
  - 방출 단위가 하나인 동안 `openapi.json` 단수명을 허용한다.
- 계약 방향은 단방향이다.
  - 백엔드가 originate 한다.
  - 프론트는 하류 소비한다.
  - 루트는 계약별 원천 하나와 단방향 흐름만 강제한다.
- tRPC는 계약 원천이 TS 서버 라우터 타입이므로 사용하지 않는다.
- ts-rest는 TS 계약·어댑터 기반이라 Spring 계약을 컴파일타임 검증하지 못하는 수기 미러가 된다.
- 방출 기계는 백엔드가 소유한다.
- 소비 기계(api-client·egress 검증)는 프론트가 소유한다.
- 계약 표면에 토큰·내부 URL을 포함하지 않는다.
- 계약 표면 모델링은 경계 양쪽에서 일관성을 유지한다.
  - seam은 존재성과 일관성만 판단한다.
  - 스킴·정책 상세는 서브가이드가 소유한다.

| 대상 | 클래스 | 비고 |
|---|---|---|
| 표면 | 버전·페이지네이션·인증·멱등성·파일 업로드·스트리밍/SSE | 계약 경계 공통 |
| 인증 정책 | 토큰 전파·egress/ingress 비대칭 | 프론트 서브가이드 소유 |

### 계약 진화·무결성

- 파괴적 변경(삭제·의미 변경)은 oasdiff로 검사한다.
- drift 게이트는 계약과 생성물의 일치를 강제한다.
  - 계약 재생성 결과와 커밋된 생성물이 다르면 CI가 실패한다.
  - 코드와 커밋된 계약(OpenAPI)의 불일치는 방출 단위 스냅샷 테스트가 검증한다.
- 기계 검증은 필요조건이며 타입 무결성 판단을 대체하지 않는다.
- seam 자체는 단계화하지 않는다.
  - 확장 소비는 원자적으로 반영한다.
  - 파괴적 제거 소비는 expand-and-contract로 단계화한다.
  - 세부 절차는 백엔드 서브가이드가 소유한다.

#### 타입이 거짓말하는 지점

- drift가 통과해도 OpenAPI가 표현하지 못하는 불변식은 사람이 검증한다.

| 대상 | 처리 | 비고 |
|---|---|---|
| UUIDv7 | plain string + 브랜드 타입·format 검증 | 생성 타입만으로 신뢰하지 않음 |
| Instant·시각 | ISO-8601 offset 문자열 | `Date` 사용 금지 |
| int64·`long` | string 또는 bigint | TS `number` 정밀도 제한 대응 |
| BigDecimal·금액 | string | 반올림 오차 방지 |
| enum | 열린 union + exhaustive 분기 | 닫힌 union은 새 서버 값에서 깨짐 |
| nullability | JSpecify 방출 확인 후 TS 매핑 검증 | `?`·`| null` 검증 |
| oneOf·판별 합집합 | discriminator 강제 | 코드젠 품질 유지 |
| ProblemDetail `code` | open string | 생성 union은 편의용 |

### 코드젠 파이프라인

- `packages/shared-types`가 계약에서 타입·Zod·API client를 생성한다.
  - orval을 사용한다.
- 코드젠 도구는 저위험·가역적이다.
  - 코드젠 도구 변경은 소비처에 영향을 주지 않는다.
- 계약마다 코드젠 엔트리 하나와 생성 디렉터리 하나를 둔다.
- 계약 간 생성 스키마 공유를 금지한다.
  - 예외: 에러 모델.
  - 생성물 공유는 소비처에서 무관한 방출 단위를 다시 결합시킨다.
- 타입은 Zod `z.infer`로 파생한다.
  - 수기 타입·수기 Zod 이중 관리를 금지한다.
- 생성물은 커밋한다.
  - 손 편집·재생성 불일치는 drift 게이트가 검출한다.
- 입력과 출력은 다음을 따른다.

| 구분 | 경로 |
|---|---|
| input | `apps/backend/docs/openapi/openapi.json` |
| output | `src/generated` |

- 계약이 여러 개면 생성 디렉터리와 패키지 export를 계약별로 분리한다.
  - 소비처는 사용하는 계약 export만 의존한다.
- 태스크 그래프는 `backend#openapi → shared-types#codegen → frontend build` 순서를 따른다.
- turbo selector는 패키지명 기준이다.
- 백엔드 `apps/backend/package.json`은 `./gradlew`로 위임한다.
  - `openapi`는 `generateOpenApiDocs`를 실행한다.
  - `verify`는 `./gradlew build`를 실행한다.
  - gradle 태스크명은 백엔드가 소유한다.
- 백엔드는 그래프상 불투명 단일 노드로 취급한다.
  - `openapi`가 모든 방출 단위 계약을 한 번에 방출한다.
  - `outputs`는 계약 파일·빌드 마커 수준으로 선언한다.
  - 내부 증분 책임은 Gradle 캐시에 둔다.
- 소비 방향은 `shared-types → api-client(server-only) → frontend app`을 따른다.
- `api-client`는 생성물을 벤더 중립 경계로 감싼다.
- 경계 규칙과 소비 정책은 프론트가 소유한다.

### 에러 모델

- 에러 계약은 RFC 9457 ProblemDetail을 사용한다.
- 형식과 확장 멤버(`code`·`errors[]`·`traceId`)는 백엔드가 소유한다.
- 공용 에러 Zod 스키마는 `packages/shared-types` 하나만 소유한다.
  - 모든 계약은 동일 에러 형상을 공유한다.
  - 계약 간 생성물 비공유의 유일한 예외다.
- 기계 분기는 `code`만 사용한다.
  - `detail`·`title` 분기를 금지한다.
- `code`는 런타임에서 open string으로 처리한다.
  - 생성 union은 편의용이며 실제 계약은 열려 있다.
- `traceId`는 관측용 확장 멤버다.
  - 로깅·관측에 사용한다.
  - 클라이언트 UI에 raw 값을 노출하지 않는다.
- 에러 소비 정책은 프론트가 소유한다.
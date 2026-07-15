# AGENTS.md — 앵커

이 저장소의 아키텍처·공유 규칙은 `docs/`의 두 문서가 소유한다. 작업 성격에 맞는 문서를 로딩한다.

## 위임 트리아지

작업의 변경 표면을 먼저 가른다.

- backend-only → `apps/backend/AGENTS.md`로 위임한다.
- frontend-only → `apps/frontend/AGENTS.md`로 위임한다.
- cross-language(seam)·root-infra → 루트가 소유한다. 아래 두 문서를 따른다.
- 서브트리 내부의 모듈·패키지·클래스는 각 서브가이드가 소유한다. 4분류 상세는 → [`docs/architecture.md`](docs/architecture.md)의 소유·위임 경계.

## 문서

- [`docs/architecture.md`](docs/architecture.md) — 구조·배치·소유(4분류)·경계 강제를 판단할 때. 트리거 상세는 그 문서 `## 언제`.
- [`docs/sharing.md`](docs/sharing.md) — 계약·코드젠·drift·에러·타입 무결성을 판단할 때. 트리거 상세는 그 문서 `## 언제`.

코드·타입 생성 후 → [`docs/architecture.md`](docs/architecture.md)의 "경계 강제"(검사가 기계로 막는 것) + [`docs/sharing.md`](docs/sharing.md)의 "타입이 거짓말하는 지점"(사람이 잡는 것)으로 자기검증한다.

언어별 작업 행동(일반 코딩 원칙)은 각 서브가이드가 소유한다.

## 편집 규약

- 아래는 `docs/`의 규칙 문서에 적용한다(이 앵커·README 같은 네비 파일은 예외).
- 문서 섹션은 제목·언제·규칙 셋으로 고정한다. 규칙은 마크다운 불릿과 표로 쓴다. HTML·이모지·강조 기호·편집자적 라벨을 넣지 않는다.
- 규칙은 상단 불릿 한 줄(명령이나 사실), 판단이 필요한 규칙의 이유·함정은 하위 불릿. 한 불릿 = 한 규칙.
- 한 규칙은 한 문서가 소유하고 나머지는 참조만 한다. 검사가 강제하는 규칙 본문은 프로즈로 재서술하지 않는다.
- 현재 상태로 쓴다. 기각한 대안 등 결정 사실은 과거형을 허용한다.

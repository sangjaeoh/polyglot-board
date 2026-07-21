# AGENTS.md — 앵커

이 저장소의 아키텍처·공유 규칙은 `docs/`의 두 문서가 소유한다. 작업 성격에 맞는 문서를 로딩한다.

## 위임 트리아지

- 작업 변경 표면을 먼저 구분한다.

| 유형 | 소유 |
|---|---|
| backend-only | `apps/backend/AGENTS.md` |
| frontend-only | `apps/frontend/AGENTS.md` |
| cross-language(seam)·root-infra | 루트 |

- 서브트리 내부 모듈·패키지·클래스는 각 서브가이드가 소유한다.
- 4분류 상세는 [`docs/architecture.md`](docs/architecture.md)의 소유·위임 경계를 따른다.

## 문서

- [`docs/architecture.md`](docs/architecture.md) — 구조, 배치, 소유, 경계 강제를 판단할 때
- [`docs/sharing.md`](docs/sharing.md) — 계약, 코드 생성, drift, 에러, 타입 무결성을 판단할 때

- 트리거 상세는 각 문서의 `## 언제`를 따른다.
- 코드·타입 생성 후 [`docs/architecture.md`](docs/architecture.md)의 경계 강제와 [`docs/sharing.md`](docs/sharing.md)의 타입 무결성 규칙으로 자기검증한다.
- 언어별 작업 행동은 각 서브가이드가 소유한다.
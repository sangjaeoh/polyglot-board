# TODO 작업 요청 프롬프트

[`todo.md`](./todo.md)의 각 항목을 새 세션에서 작업 요청할 때 쓰는 프롬프트다. 항목 하나를 골라 코드 블록 안 내용을 그대로 붙여넣는다. 프롬프트는 자기완결이라 이 파일의 다른 부분 없이 단독으로 동작한다(레포 규칙은 CLAUDE.md→AGENTS.md가 자동 적용되며, 프론트 작업은 `apps/frontend/AGENTS.md`와 `apps/frontend/docs/`가 소유한다). 각 프롬프트는 마지막에 todo.md 체크 갱신 단계를 포함한다. 항목 번호 순서가 곧 권장 실행 순서이며 순서 근거는 todo.md가 소유한다.

각 프롬프트의 파일 경로·설정 형태는 제안이다 — 기존 관례·docs 규칙과 충돌하면 docs를 따른다. 구현이 `apps/frontend/docs/`의 선언과 충돌하는 항목은 해당 선언의 갱신이 각 작업의 일부이며, 문서 편집은 편집 규약(제목·언제·규칙, 한 불릿 = 한 규칙, 범용 문구)을 따른다.

## 1. 토큰 스타일시트 소유권 이동

```text
[작업] 토큰 스타일시트를 packages/ui로 이동 — 디자인시스템 패키지가 토큰을 소유한다

배경(확인된 사실):
- 토큰 정의 전체(라이트/다크 팔레트 :root 블록 + @theme inline 매핑 + 브랜드 선택 근거 주석)가 apps/frontend/apps/web/src/app/globals.css에 있다. packages/ui 컴포넌트(Button 등)는 bg-primary·text-ink 등 시맨틱 클래스를 소비하므로, 앱이 정의해 줄 토큰에 기대는 보이지 않는 계약 상태다.
- apps/frontend/docs/design-system.md 토큰 규칙: "토큰 정의 스타일시트는 디자인시스템 패키지가 소유하고 각 앱이 import한다. 앱마다 재정의·복제하지 않는다."
- packages/ui의 package.json exports는 "./src/index.ts" 하나라 CSS 노출 엔트리가 없다.
- Tailwind v4 빌드는 앱 postcss에서 돌고, globals.css가 @source '../../../../packages/ui/src'로 ui 클래스를 스캔한다 — 패키지의 토큰 CSS를 앱이 @import하면 앱 빌드가 처리한다.
- 폰트 변수(--font-plex-sans-kr·--font-plex-mono)는 앱 layout의 next/font/local이 주입하고, 토큰 --font-sans/--font-mono는 그 변수 + 폴백 스택을 참조한다.
- globals.css에는 토큰이 아닌 앱 소유 CSS도 있다: 라우트 진행 바, @layer base(body 기본), reduced-motion 전역, ::selection.

목표: packages/ui가 토큰 정의를 소유하고 앱은 import만 하며, 라이트/다크 모두 화면 시각이 이동 전과 동일하다.

작업 내용:
1. packages/ui/src/tokens.css 신설 — 팔레트(:root + 다크 media query)와 @theme inline 매핑을 globals.css에서 값 변경 없이 그대로 이동한다. 브랜드 선택 근거 주석도 함께 옮긴다(토큰 값의 근거는 정의처 주석이 소유 — design-system.md).
2. packages/ui package.json exports에 CSS 서브패스를 노출한다(예: "./tokens.css"). 소스 직접 노출 vs 빌드 산출 노출을 결정하고 근거를 남긴다(현 패키지는 무빌드 소스 직참조 구조).
3. globals.css는 토큰 import + 앱 소유분(라우트 진행 바·base·reduced-motion·selection)만 남긴다. @source 스캔이 계속 동작하는지 확인한다.
4. 폰트는 이동하지 않는다 — next/font 변수 주입은 앱 소유로 유지하고, 토큰이 변수 참조+폴백 스택이라 패키지 이동에 문제없음을 확인만 한다.
5. 검증: 루트 pnpm verify 통과 + 실제 기동해 라이트/다크 두 테마의 화면이 이동 전과 동일함을 확인한다(다크는 OS 설정 또는 에뮬레이션).

하지 말 것: 토큰 값·이름 변경, 신규 토큰 추가(2번 항목), next/font 로딩 이동, CSS 구조 재설계.

완료 기준: 시각 동일 확인 기록이 남고 pnpm verify가 통과한다.
완료 후: 루트 todo.md의 1번 항목(토큰 스타일시트 소유권 이동)을 체크([ ] → [x])로 갱신한다. 커밋 및 메인머지, 잔여브랜치 삭제
```

## 2. 토큰 확장 — 타이포 롤·모션·레이어

```text
[작업] 토큰 확장 — 타이포 롤·모션·레이어 토큰 정의와 소비 치환

배경(확인된 사실):
- apps/frontend/docs/design-system.md 분류표는 색·타이포·스페이싱·radius/shadow·모션·레이어·브레이크포인트를 표준으로 규정하나, 배선된 것은 색·radius·shadow·폰트 패밀리뿐이다(packages/ui/src/tokens.css — 1번 항목 완료 전제).
- 같은 문서의 규칙 "타이포는 롤 토큰 단위로 소비한다. 크기·행간·굵기를 유틸리티로 개별 조합해 롤을 재조립하지 않는다"에 대해, 현재 전 컴포넌트가 text-sm/text-base/text-xs + font-medium을 개별 조합한다(packages/ui의 Button·TextField·TextAreaField, features/board/ui의 PostCard·PostList·PostForm·PostTime 등).
- 모션은 duration-150/duration-200 원시 유틸을 쓰고 easing 토큰이 없다. 레이어는 라우트 진행 바가 z-index 50을 직접 쓴다.
- Tailwind v4는 @theme 네임스페이스(--text-*·--font-weight-*·--ease-*·--z-* 등)로 토큰을 유틸리티에 매핑한다.

목표: 화면에 실재하는 텍스트 위계가 롤 토큰으로, 모션·레이어가 단계 토큰으로 정의되고, 전 컴포넌트의 개별 조합 소비가 토큰 소비로 치환되며, 시각 결과는 동일하다.

작업 내용:
1. 타이포 감사: 현 화면의 텍스트 위계를 전수 목록화한다(제목·본문·라벨·캡션/메타·모노 캡션 등 — 실제 쓰는 조합만). 화면에 없는 롤을 만들지 않는다(투기 금지).
2. 롤 토큰을 역할 기반 이름으로 @theme에 정의하고(크기·행간·굵기 묶음), 기존 소비를 치환한다. Tailwind v4의 --text-* 네임스페이스 특성(크기+행간 페어 지원)을 활용한다.
3. 모션 duration·easing 단계, 레이어 단계 토큰을 정의하고 duration-150류·z-50류 소비를 치환한다.
4. 스페이싱·브레이크포인트는 결정으로 종결한다(제안: Tailwind 기본 스케일을 토큰 스케일로 수용한다고 기록 — 별도 단계 토큰은 필요가 생길 때. 다르게 판단하면 근거를 남긴다). 결정이 design-system.md 분류표 서술과 어긋나면 문서를 같이 갱신한다(범용 문구 유지).
5. 검증: 루트 pnpm verify + 실제 기동해 양 테마 시각 동일 확인.

하지 말 것: 값 리디자인(크기·색 변경 — 현 시각의 토큰화가 목적), 컴포넌트 prop API 변경, 사용처 없는 롤·단계 추가.

완료 기준: 타이포·모션·레이어의 개별 유틸 조합이 코드에서 사라지고(치환 완료), pnpm verify와 시각 확인이 통과한다.
완료 후: 루트 todo.md의 2번 항목(토큰 확장)을 체크([ ] → [x])로 갱신한다. 커밋 및 메인머지, 잔여브랜치 삭제
```

## 3. 토큰 우회 차단 린트 배선

```text
[작업] 토큰 우회 차단 린트 배선 — 문서 선언과 강제 장치의 정합

배경(확인된 사실):
- apps/frontend/docs/code-quality.md는 "원시 색·치수 값(토큰 우회)도 여기서 막는다(ESLint)"고 선언하나, apps/web/eslint.config.mjs에는 해당 규칙이 없다(next·react-hooks·jsx-a11y·boundaries만). AGENTS.md의 "강제 장치가 아직 배선되지 않은 레포에서는 자기검증이 유일한 게이트다" 단서가 현재의 유일한 가드다.
- packages/ui에는 lint 스크립트 자체가 없다(package.json scripts: typecheck만) — 디자인시스템 패키지가 무린트다. turbo lint 태스크 그래프는 있으나 ui가 참여하지 않는다.
- 차단 대상 후보: Tailwind 임의 값 구문의 색·치수(bg-[#...]·w-[Npx] 등), 비토큰 색 팔레트 유틸(bg-red-500류). 현 코드의 임의 값 사용처(PostCard의 w-[3px]·PostList 빈 상태의 w-[3px] 등)는 판단 대상이다 — 토큰화 vs 허용 예외.
- code-quality.md 새 의존성 규칙: 프론트 의존성 도입은 신중히 한다(단, 린트는 devDependency라 런타임 번들 무영향).

목표: 원시 색·치수 값 사용이 린트 에러로 막히고(packages/ui 포함), 문서 선언과 배선이 일치한다.

작업 내용:
1. 구현 수단 결정(트레이드오프 명시): no-restricted-syntax 정규식(의존성 0, 패턴 직접 관리) vs 서드파티 플러그인(관리 위임, Tailwind v4 호환 상태 확인 필요). 제안: 의존성 0의 정규식으로 시작 — 다르게 판단하면 근거를 남긴다.
2. 차단 범위를 정의하고 근거를 남긴다: 임의 값 구문의 색(hex·rgb)·치수(px)는 차단, 비토큰 색 팔레트 유틸 차단. 정당한 예외가 있으면(예: 장식용 미세 치수) 억제 규칙(최소 스코프 + 이유 주석 — code-quality.md의 억제)을 따르게 한다.
3. packages/ui 린트 참여를 배선한다: 배치를 결정하고(앱 config의 files 확장 vs ui 인라인 flat config) 근거를 남긴다 — code-quality.md의 강제 기계 배치 서술과 어긋나면 문서를 같이 갱신한다(범용 문구 유지).
4. 기존 위반을 정리한다(2번 항목 완료 전제 — 롤 토큰 치환 후 남는 위반만).
5. 검증: 위반 샘플을 임시로 넣어 레드를 확인하고 제거 → 루트 pnpm verify 그린.

하지 말 것: 전체 스타일 재작성, Tailwind 설정 체계 개편, 이번 범위 밖 린트 규칙 추가.

완료 기준: 위반 샘플 레드 확인 기록이 남고, packages/ui가 lint 그래프에 참여하며, pnpm verify가 통과한다.
완료 후: 루트 todo.md의 3번 항목(토큰 우회 차단 린트 배선)을 체크([ ] → [x])로 갱신한다. 커밋 및 메인머지, 잔여브랜치 삭제
```

## 4. 컴포넌트 인벤토리 감사·승격

```text
[작업] 컴포넌트 인벤토리 감사·승격 — 실증된 중복의 디자인시스템 흡수

배경(확인된 사실):
- packages/ui는 프리미티브 3개(Button·TextField·TextAreaField), features/board/ui는 6개(PostCard·PostForm·PostList·PostTime·DeletePostButton·SubmitButton)다.
- 실증된 중복 — 버튼 시각의 링크: PostList 빈 상태 CTA(Link)가 Button primary의 클래스 문자열(bg-primary·px-4 py-2.5·shadow-control·hover:bg-primary-hover 등)을 그대로 복제하고, 페이지네이션 링크(pageLinkClass)는 secondary 유사 스타일을 별도 문자열로 중복한다. Button은 <button> 전용이라 링크가 재사용할 수 없다.
- 미검증 후보(사용처 수 확인 필요): 빈 상태 셸(PostList 빈 상태 — not-found.tsx·error.tsx와 비교), 카드 셸(PostCard 인라인 — 상세 페이지와 비교), 인라인 경고(PostForm의 state.message 박스).
- apps/frontend/docs/design-system.md 승격 기준: "도메인 어휘 없이 표현할 수 있고 둘째 사용처가 생길 때 승격한다. 사용처가 하나인 범용화는 투기다." 완료 정의: 상태 완비·양 테마·모드 분기 없음·모션 토큰·원시값 없음·(5번 배선 후) 스토리.
- coding-conventions.md 접근성 규칙: 시맨틱 HTML 우선 — 링크는 링크(a), 버튼은 button.

목표: 승격 기준을 충족하는 패턴이 도메인 무지 컴포넌트로 packages/ui에 흡수되고, 미충족 패턴은 근거와 함께 종결된다.

작업 내용:
1. 인벤토리 감사: 전 tsx의 반복 시각 패턴을 사용처 수와 함께 목록화하고 결과를 PR 설명에 남긴다(감사 자체가 산출물).
2. 버튼 시각 링크(기준 충족 실증분) 처리: 형태를 결정하고 트레이드오프를 명시한다 — 후보: (a) 스타일 산출을 공유하는 ButtonLink 컴포넌트 신설, (b) Button의 render 위임(asChild류), (c) variant 클래스 함수 export. 시맨틱은 유지한다(링크는 a 태그 — div/button 흉내 금지). 기존 중복 문자열(PostList CTA·페이지네이션)을 신설물 소비로 치환한다.
3. 나머지 후보는 사용처를 센다: 둘 이상 + 도메인 무지 표현 가능이면 조합 컴포넌트로 승격(콘텐츠는 슬롯으로 — design-system.md), 하나뿐이면 "승격 없음 + 근거"로 종결한다(그것도 유효한 완료다).
4. 승격분은 도메인 어휘를 역할 어휘로 치환하고(Post류 이름 금지), 완료 정의로 자기검증한다.
5. 검증: 루트 pnpm verify + 실제 기동해 목록·빈 상태·페이지네이션의 시각 동일 확인.

하지 말 것: 사용처 하나인 패턴의 투기적 승격, 시각 리디자인, packages/ui에 도메인 로직·타입 유입, feature 컴포넌트 구조 개편.

완료 기준: 감사 목록이 남고, 실증 중복이 해소되며(클래스 문자열 복제 소멸), pnpm verify와 시각 확인이 통과한다.
완료 후: 루트 todo.md의 4번 항목(컴포넌트 인벤토리 감사·승격)을 체크([ ] → [x])로 갱신한다. 커밋 및 메인머지, 잔여브랜치 삭제
```

## 5. Storybook 배선

```text
[작업] Storybook 워크벤치 배선 — 스토리 계약과 게이트 합류

배경(확인된 사실):
- 레포에 *.stories.* 파일이 0개다. apps/frontend/docs/code-quality.md Storybook 섹션이 규정한다: 워크벤치는 Storybook(CSF3), 스토리는 컴포넌트 옆 co-locate, 설정은 디자인시스템 패키지 소유(앱에 두지 않는다), 게이트(전 스토리 렌더 스모크 test-runner + 접근성 addon-a11y·axe)를 루트 verify 파이프라인에 포함, 시각 회귀 도구는 선제 도입 금지.
- design-system.md 워크벤치 규칙: variant × 상태 × 테마 매트릭스, 스토리가 사용 계약, 강제 범위는 디자인시스템 컴포넌트만(도메인 UI 비강제), 완료 정의에 스토리 커버 포함.
- 루트 verify는 pnpm verify = turbo verify(lint·typecheck 의존) + depcruise + drift 체크다. ui 패키지가 참여할 turbo 태스크 그래프가 이미 있다.
- 1번 항목(토큰 이동) 완료가 구조적 전제다 — packages→apps 의존은 금지 방향이라 토큰이 앱에 있으면 프리뷰가 스타일 없이 렌더된다. 프리뷰는 packages/ui의 tokens.css를 import한다.
- Tailwind v4 처리는 앱 postcss가 소유하므로 Storybook은 자체 빌드에서 Tailwind 처리 배선이 필요하다. 다크 테마는 prefers-color-scheme 전환이라 프리뷰에서 양 테마 확인 수단이 필요하다. 폰트 변수는 앱 next/font 주입이라 프리뷰에는 없다 — 토큰의 폴백 스택으로 렌더된다.

목표: packages/ui의 모든 컴포넌트가 variant × 상태 × 테마 스토리를 갖고, 스토리 렌더 스모크와 a11y 검사가 루트 verify에서 강제된다.

작업 내용:
1. 빌더 결정(트레이드오프 명시): 제안은 @storybook/react-vite + @tailwindcss/vite — packages/ui는 Next 밖 순수 React 패키지라 next 빌더는 앱 결합을 만든다. 다르게 판단하면 근거를 남긴다. 의존성은 전부 devDependencies로 두고 런타임 번들 무영향 근거를 명시한다(code-quality.md 새 의존성).
2. packages/ui에 .storybook 설정을 소유시키고, preview에서 tokens.css를 import한다. 양 테마 확인 수단을 결정한다(예: color-scheme 강제 데코레이터/툴바 — prefers-color-scheme 에뮬레이션의 한계를 명시하고 결정 근거를 남긴다).
3. 스토리 작성(co-locate, CSF3): 기존 프리미티브 3개 + 4번 승격분. 각각 variant 전수 × 상태(기본·hover·focus-visible·disabled·에러 해당분) × 테마를 커버한다. 스토리 작성 중 완료 정의 미달(누락 상태 등)이 드러나면 해당 컴포넌트를 보수한다 — 그것이 워크벤치의 존재 이유다.
4. 게이트 배선: test-runner 스모크 + addon-a11y 검사를 packages/ui 스크립트로 만들고 turbo 그래프를 통해 루트 pnpm verify에 합류시킨다(CI 없음 전제 — 로컬 verify가 게이트라는 기존 결정과 정합).
5. 검증: 전 스토리 그린 + a11y 통과 + 루트 pnpm verify 그린. 고의로 스토리 하나를 깨뜨려 verify가 레드가 되는지 확인 후 복원한다(게이트 실효성 확인).

하지 말 것: 시각 회귀 도구 도입(문서가 선제 도입을 금지 — 도입 시점에 code-quality.md가 고정), features/ 도메인 UI 스토리 강제, 앱에 Storybook 설정 배치.

완료 기준: 게이트 실효성 확인 기록(레드→그린)이 남고 pnpm verify가 통과하며, code-quality.md·design-system.md의 워크벤치 선언과 배선이 일치한다.
완료 후: 루트 todo.md의 5번 항목(Storybook 배선)을 체크([ ] → [x])로 갱신한다. 커밋 및 메인머지, 잔여브랜치 삭제
```

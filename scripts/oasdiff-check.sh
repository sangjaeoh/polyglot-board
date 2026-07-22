#!/bin/sh
# oasdiff 게이트: base(merge-base) 대비 계약 파괴적 변경을 검사한다 (docs/sharing.md).
# --fail-on ERR: 확정 파괴(ERR)만 실패 처리. WARN 승격과 의도적 breaking 처리(err-ignore)는
# T8(페이지네이션 1-based 전환)에서 함께 판단한다.
# oasdiff 버전 핀이 .mise.toml 밖의 제2 채널인 것은 인지된 트레이드오프다(mise 레지스트리 부재로 Docker 사용).
set -eu
cd "$(git rev-parse --show-toplevel)"
CONTRACT=apps/backend/docs/openapi/openapi.json
BASE_REF=${OASDIFF_BASE:-origin/main}
base=$(git merge-base HEAD "$BASE_REF")
if git cat-file -e "$base:$CONTRACT" 2>/dev/null; then
  echo "oasdiff:check: base=$base ($BASE_REF) 대비 breaking 검사"
  git show "$base:$CONTRACT" | docker run --rm -i \
    -v "$PWD/$CONTRACT":/head/openapi.json:ro \
    tufin/oasdiff:v1.24.0@sha256:5c8447b0adc2647b7c277348c540136e3c2145aa80bb3846acc26f9b7ff2a6d7 \
    breaking --fail-on ERR - /head/openapi.json
else
  echo "oasdiff:check: base($base)에 계약 파일 없음 - 신규 계약으로 간주, 통과"
fi

#!/bin/bash
# docs/openapi.json 의 operationId 집합이 스냅샷(docs/operationIds.txt)과 일치하는지 검사한다.
# operationId 는 메서드명에서 암묵 파생되므로, 메서드 리네임 시 조용히 바뀌어
# FE 클라이언트 함수명(2단계 매핑) 추적이 끊길 수 있다. 이를 가시화하기 위한 가드다.
#
# 현재는 warning 모드(exit 0)다. PR 블로킹이 필요하면 마지막 exit 0 -> exit 1 로 변경.
set -euo pipefail

SPEC="docs/openapi.json"
SNAPSHOT="docs/operationIds.txt"
CURRENT="$(mktemp)"
trap 'rm -f "$CURRENT"' EXIT

if [[ ! -f "$SPEC" ]]; then
  echo "::error::$SPEC 가 없습니다. 먼저 스펙을 갱신하세요."
  exit 1
fi

jq -r '.paths | to_entries[] | .value | to_entries[] | (.value.operationId // empty)' "$SPEC" | sort > "$CURRENT"

if ! diff -q "$SNAPSHOT" "$CURRENT" > /dev/null 2>&1; then
  echo "::warning::operationId 변경 감지됨. 의도한 변경인지 확인하세요."
  echo "변경 내역 (< 스냅샷 / > 현재):"
  diff "$SNAPSHOT" "$CURRENT" || true
  echo ""
  echo "의도한 변경이라면 다음 명령으로 스냅샷을 갱신하세요:"
  echo "  jq -r '.paths | to_entries[] | .value | to_entries[] | (.value.operationId // empty)' $SPEC | sort > $SNAPSHOT"
  exit 0  # warning 모드 — 블로킹하려면 exit 1
fi

echo "operationId 변경 없음 ✓ (${SNAPSHOT})"

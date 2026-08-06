#!/usr/bin/env bash
# Bandage E2E 테스트 데이터 복원 스크립트
#
# DB를 초기화한 뒤 이 스크립트를 실행하면 docs/E2E-TEST-SPEC.md 에 명세된 상태로 복원된다.
# API 를 통해 재생성하므로 UUID·회원 ID 값 자체는 새로 발급되지만,
# 데이터의 구조·규모·관계는 spec.py 의 고정 시드(20260806)로 동일하게 재현된다.
#
# 사용법:
#   ./restore.sh                                   # 기본: https://bandage.team
#   BANDAGE_BASE=http://localhost:8080/api/v1 ./restore.sh
#   ./restore.sh --clean                           # 기존 밴드 삭제 후 복원
#
# ponytail: Liquibase changeset 대신 API 재생성. UUIDv7 PK 와 회원 시퀀스가 서버 생성이라
#           SQL 덤프는 재현성이 깨진다. 상태 재현이 목적이면 이쪽이 정확하다.

set -euo pipefail
cd "$(dirname "$0")"

export BANDAGE_BASE="${BANDAGE_BASE:-https://bandage.team/api/v1}"
export SEED_STATE="${SEED_STATE:-/tmp/bandage_seed_state.json}"
POSTER_SRC="../../poster-images"
POSTER_OUT="/tmp/posters_resized"

echo "=== Bandage E2E 데이터 복원 ==="
echo "대상 서버: $BANDAGE_BASE"
echo "상태 파일: $SEED_STATE"
echo

command -v python3 >/dev/null || { echo "python3 필요"; exit 1; }

# 서버 응답 확인
if ! curl -sf -o /dev/null --max-time 15 "${BANDAGE_BASE%/api/v1}/" 2>/dev/null; then
  echo "경고: 서버 응답을 확인하지 못했습니다. 계속 진행합니다."
fi

if [ "${1:-}" = "--clean" ]; then
  echo "[0/4] 기존 밴드 정리"
  python3 cleanup.py
  echo
fi

# 5MB 초과 포스터 리사이즈 (서버 한도 초과분 — 장애보고서 D-04 참조 항목)
echo "[1/4] 포스터 이미지 준비 (5MB 초과분 리사이즈)"
mkdir -p "$POSTER_OUT"
for f in "$POSTER_SRC"/*; do
  [ -f "$f" ] || continue
  b=$(basename "$f")
  sz=$(stat -f%z "$f" 2>/dev/null || stat -c%s "$f")
  if [ "$sz" -gt 5000000 ]; then
    out="$POSTER_OUT/${b%.*}.jpg"
    if command -v sips >/dev/null; then
      sips -s format jpeg -Z 2000 "$f" --out "$out" >/dev/null 2>&1
    elif command -v convert >/dev/null; then
      convert "$f" -resize 2000x2000\> -quality 85 "$out"
    else
      echo "  경고: sips/convert 없음 — $b 건너뜀 (5MB 초과로 업로드 실패 예상)"
      continue
    fi
    echo "  리사이즈 $b ($sz → $(stat -f%z "$out" 2>/dev/null || stat -c%s "$out") bytes)"
  else
    cp "$f" "$POSTER_OUT/$b"
  fi
done
echo

echo "[2/4] 데이터 시드 (회원 120 / 밴드 6 / 셋리스트 60 × 15곡 / 공연 18)"
echo "      API 약 10,000건, 8분 내외 소요됩니다."
python3 seed.py
echo

echo "[3/5] 보정 (포스터 재등록 + 위임 시나리오)"
python3 fixup.py
echo

# 기존 계정이 구 비밀번호(pw1234)로 남아 있을 경우에만 실제로 변경된다.
echo "[4/5] 비밀번호 정규화 (프론트 규약: 8자 이상)"
python3 change_password.py || echo "  일부 계정 변경 실패 — 위 로그 확인"
echo

echo "[5/5] 검증 (API 138 케이스)"
python3 verify.py || echo "  검증 FAIL 존재 — docs/E2E-FAILURE-REPORT.md 의 알려진 결함과 대조하세요."
echo

echo "=== 복원 완료 ==="
python3 - <<'PY'
import json, os
S = json.load(open(os.environ["SEED_STATE"]))
print(f"  회원 {len(S['members'])} / 밴드 {len(S['bands'])} / 셋리스트 {len(S['setlists'])} "
      f"/ 트랙 {sum(s['trackCount'] for s in S['setlists'])} / 공연 {len(S['performances'])} / 포스터 {len(S['posters'])}")
print(f"  상태 파일: {os.environ['SEED_STATE']}")
print("  계정: member1~member120 / 비밀번호 12345678")
PY

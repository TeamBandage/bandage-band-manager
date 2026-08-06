"""전 테스트 계정 비밀번호 변경 (pw1234 -> 12345678).

프론트 비밀번호 규약(8자 이상)에 맞추기 위함.
재실행 안전: 이미 새 비밀번호로 바뀐 계정은 건너뛴다.
"""

import json
import os
import sys
from concurrent.futures import ThreadPoolExecutor

import api
import spec

OLD = os.environ.get("OLD_PW", "pw1234")
NEW = os.environ.get("NEW_PW", "12345678")
STATE_PATH = os.environ.get("SEED_STATE", "/tmp/bandage_seed_state.json")

S = json.load(open(STATE_PATH))
members = sorted(S["members"].items(), key=lambda kv: int(kv[0]))
print(f"대상 {len(members)}명: {OLD!r} -> {NEW!r}")

done, skipped, failed = [], [], []


def change(item):
    idx, m = item
    em = m["email"]
    # 이미 새 비밀번호면 건너뛴다
    try:
        api.login(em, NEW)
        return ("skip", idx, em, "이미 변경됨")
    except Exception:
        pass
    try:
        t = api.login(em, OLD)
    except Exception as e:
        return ("fail", idx, em, f"기존 비밀번호 로그인 실패: {str(e)[:120]}")
    st, body = api.call("PATCH", "/auth/password", token=t,
                        body={"originalPassword": OLD, "newPassword": NEW}, expect="any")
    if not (200 <= st < 300):
        return ("fail", idx, em, f"변경 실패 {st}: {str(body)[:120]}")
    try:
        api.login(em, NEW)  # 새 비밀번호 검증
    except Exception as e:
        return ("fail", idx, em, f"변경 후 로그인 실패: {str(e)[:120]}")
    return ("ok", idx, em, "")


with ThreadPoolExecutor(max_workers=8) as ex:
    for kind, idx, em, msg in ex.map(change, members):
        if kind == "ok":
            done.append(idx)
        elif kind == "skip":
            skipped.append(idx)
        else:
            failed.append((idx, em, msg))
            print(f"  FAIL member{idx}: {msg}")

print(f"\n변경 {len(done)} / 건너뜀 {len(skipped)} / 실패 {len(failed)}")

if failed:
    print("실패 목록:")
    for idx, em, msg in failed:
        print(f"  member{idx} ({em}): {msg}")
    sys.exit(1)

# 상태 파일에 새 토큰 반영 (verify.py 등이 재사용)
print("토큰 갱신 중...")
new_tok = {}


def relogin(item):
    idx, m = item
    return m["memberId"], api.login(m["email"], NEW)


with ThreadPoolExecutor(max_workers=8) as ex:
    for mid, t in ex.map(relogin, members):
        new_tok[str(mid)] = t

S["tokens"] = new_tok
S["password"] = NEW
json.dump(S, open(STATE_PATH, "w"), ensure_ascii=False, indent=1)
print(f"완료: 전 계정 비밀번호 {NEW!r}, 토큰 {len(new_tok)}개 갱신")

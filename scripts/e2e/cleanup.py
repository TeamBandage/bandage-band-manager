"""이전 실행 잔여 밴드 삭제. 각 밴드의 리더 토큰이 필요하므로 후보 계정을 순회한다."""

import sys

import api

# 잔여 정리 대상 계정 후보(이 세션에서 만든 것들)
CANDIDATES = ["smoke1@bandage.test"] + [f"probep{i}_1@bandage.test" for i in range(1, 5)] + \
             [f"probe{s}_{i}@bandage.test" for s in ("p1", "p2", "p3", "p4") for i in range(1, 5)] + \
             [f"member{i}@bandage.test" for i in range(1, 200)] + \
             [f"member{i}.dry@bandage.test" for i in range(1, 30)] + \
             [f"member{i}.dry2@bandage.test" for i in range(1, 30)]

targets = sys.argv[1:] if len(sys.argv) > 1 else None
tokens = []
for em in CANDIDATES:
    try:
        tokens.append(api.login(em))
    except Exception:
        pass
print(f"로그인 가능 계정 {len(tokens)}개")

if not tokens:
    sys.exit("삭제 권한 계정 없음")

bands = api.call("GET", "/bands", token=tokens[0], params={"pageSize": 100})["content"]
print(f"서버 밴드 {len(bands)}개")

deleted, failed = 0, []
for b in bands:
    name = b.get("bandName") or b.get("name")
    if targets and name not in targets:
        continue
    ok = False
    for t in tokens:
        st, _ = api.call("DELETE", f"/bands/{b['bandId']}", token=t, expect=[200, 204, 403, 404])
        if st in (200, 204):
            ok = True
            break
    if ok:
        deleted += 1
        print(f"  삭제 {name}")
    else:
        failed.append(name)

print(f"\n삭제 {deleted}건, 실패 {failed}")

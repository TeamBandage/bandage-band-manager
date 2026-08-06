"""시드 후 보정: 누락 포스터 재등록(리사이즈본) + 위임 시나리오 올바른 필드로 재실행."""

import json
import os

import api

STATE_PATH = os.environ.get("SEED_STATE", "/tmp/bandage_seed_state.json")
S = json.load(open(STATE_PATH))
tok = S["tokens"]
DIR = "/tmp/posters_resized"
imgs = sorted(os.listdir(DIR))

print("== 0. 밴드 프로필 이미지 누락분 (5MB 초과로 실패한 밴드) ==")
for sb in S["bands"]:
    if sb.get("profileImg"):
        continue
    f = os.path.join(DIR, f"poster{sb['no']}.jpg")
    if not os.path.exists(f):
        f = os.path.join(DIR, imgs[sb["no"] % len(imgs)])
    ext = f.rsplit(".", 1)[-1].lower()
    ct = "image/png" if ext == "png" else "image/jpeg"
    t = tok[str(sb["leaderMemberId"])]
    try:
        pre = api.call("POST", f"/bands/{sb['bandId']}/profile-image/presigned-url", token=t,
                       body={"contentLength": os.path.getsize(f), "contentType": ct, "ext": ext})
        pre["contentType"] = ct
        key = api.upload_image(pre, f)
        api.call("PATCH", f"/bands/{sb['bandId']}", token=t, body={"profileImg": key})
        sb["profileImg"] = key
        print(f"   OK {sb['name']} <- {os.path.basename(f)}")
    except Exception as e:
        print(f"   FAIL {sb['name']}: {str(e)[:200]}")

print("== 1. 포스터 누락분 재등록 ==")
have = {p["performanceId"] for p in S["posters"]}
missing = [p for p in S["performances"] if p["performanceId"] not in have]
print(f"   누락 {len(missing)}건")
for i, p in enumerate(missing):
    f = os.path.join(DIR, imgs[(len(have) + i) % len(imgs)])
    ext = f.rsplit(".", 1)[-1].lower()
    ct = "image/png" if ext == "png" else "image/jpeg"
    t = tok[str(p["ownerMemberId"])]
    try:
        pre = api.call("POST", "/performance-posters/presigned-url", token=t,
                       params={"performanceId": p["performanceId"]},
                       body={"contentLength": os.path.getsize(f), "contentType": ct, "ext": ext})
        pre["contentType"] = ct
        key = api.upload_image(pre, f)
        po = api.call("POST", "/performance-posters", token=t,
                      body={"performanceId": p["performanceId"], "imageKey": key,
                            "description": f"{p['title']} 포스터"})
        S["posters"].append({"posterId": api.pick_id(po, "posterId", "id"),
                             "performanceId": p["performanceId"], "image": os.path.basename(f)})
        print(f"   OK {p['title']} <- {os.path.basename(f)}")
    except Exception as e:
        print(f"   FAIL {p['title']}: {str(e)[:200]}")

# poster-images 전량이 최소 1회 쓰였는지 확인
used = {x["image"] for x in S["posters"]}
unused = sorted(set(imgs) - used)
# 요구사항: poster-images 전량을 최소 1회 사용 → 남은 이미지는 임의 공연에 추가 등록
for i, name in enumerate(unused):
    p = S["performances"][i % len(S["performances"])]
    f = os.path.join(DIR, name)
    ext = name.rsplit(".", 1)[-1].lower()
    ct = "image/png" if ext == "png" else "image/jpeg"
    t = tok[str(p["ownerMemberId"])]
    try:
        pre = api.call("POST", "/performance-posters/presigned-url", token=t,
                       params={"performanceId": p["performanceId"]},
                       body={"contentLength": os.path.getsize(f), "contentType": ct, "ext": ext})
        pre["contentType"] = ct
        key = api.upload_image(pre, f)
        po = api.call("POST", "/performance-posters", token=t,
                      body={"performanceId": p["performanceId"], "imageKey": key,
                            "description": f"{p['title']} 포스터 ({name})"})
        S["posters"].append({"posterId": api.pick_id(po, "posterId", "id"),
                             "performanceId": p["performanceId"], "image": name})
        print(f"   OK 미사용 이미지 {name} -> {p['title']}")
    except Exception as e:
        print(f"   FAIL {name}: {str(e)[:200]}")
print("   사용된 이미지:", sorted({x["image"] for x in S["posters"]}))

print("== 2. 위임 시나리오 (올바른 필드명) ==")
scen = S.get("band6_scenarios", [])
# 재실행 안전장치: 이미 성공 기록이 있으면 건너뛴다(반복 위임으로 상태가 계속 바뀌는 것 방지)
if any("재실행" in x.get("case", "") and x.get("result") == "성공" for x in scen):
    print("   이미 수행됨 — 건너뜁니다.")
    S["fixupApiSummary"] = api.summary()
    json.dump(S, open(STATE_PATH, "w"), ensure_ascii=False, indent=1)
    print(f"\n포스터 총 {len(S['posters'])}개 / 공연 {len(S['performances'])}개")
    raise SystemExit(0)
b6no = max(b["no"] for b in S["bands"])

# 셋리스트 매니저 위임: managerId
s6 = [s for s in S["setlists"] if s["band"] == b6no]
if s6:
    s = s6[0]
    cand = [p for p in s["participants"] if p != s["managerId"]]
    if cand:
        try:
            api.call("PATCH", f"/setlists/{s['setlistId']}/manager", token=tok[str(s["managerId"])],
                     body={"managerId": cand[0]})
            print(f"   셋리스트 매니저 위임 성공: {s['managerId']} -> {cand[0]}")
            scen.append({"case": "셋리스트 매니저 위임(재실행)", "from": s["managerId"], "to": cand[0], "result": "성공"})
            s["managerId"] = cand[0]
        except api.ApiError as e:
            print(f"   실패: {e}")
            scen.append({"case": "셋리스트 매니저 위임(재실행)", "result": f"실패 {e.status} {e.body}"})

# 공연 오너 위임: targetMemberId (대상은 해당 공연의 MANAGER 여야 함 → 초대 수락한 파트너 리더)
p6 = [p for p in S["performances"] if p["band"] == b6no and p["partnerBands"]]
if p6:
    p = p6[0]
    band_by_no = {b["no"]: b for b in S["bands"]}
    target = band_by_no[p["partnerBands"][0]]["leaderMemberId"]
    try:
        api.call("PATCH", f"/performances/{p['performanceId']}/owner", token=tok[str(p["ownerMemberId"])],
                 body={"targetMemberId": target})
        print(f"   공연 오너 위임 성공: {p['ownerMemberId']} -> {target}")
        scen.append({"case": "연합공연 오너 위임(재실행)", "from": p["ownerMemberId"], "to": target, "result": "성공"})
        p["ownerMemberId"] = target
    except api.ApiError as e:
        print(f"   실패: {e}")
        scen.append({"case": "연합공연 오너 위임(재실행)", "result": f"실패 {e.status} {str(e.body)[:200]}"})

# 밴드6 리더 위임 후 리더 탈퇴 (매니저 중도 이탈 시나리오)
b6 = [b for b in S["bands"] if b["no"] == b6no][0]
try:
    bms = api.call("GET", f"/bands/{b6['bandId']}/members", token=tok[str(b6["leaderMemberId"])], params={"pageSize": 100})
    others = [x for x in bms.get("content", []) if x.get("memberId") != b6["leaderMemberId"]]
    if others:
        tgt = others[0]
        bmid = api.pick_id(tgt, "bandMemberId", "id")
        api.call("PATCH", f"/bands/{b6['bandId']}/members/{bmid}/role", token=tok[str(b6["leaderMemberId"])],
                 body={"role": "LEADER"})
        print(f"   밴드6 리더 위임 성공 -> {tgt.get('memberId')}")
        scen.append({"case": "밴드 리더 위임", "from": b6["leaderMemberId"], "to": tgt.get("memberId"), "result": "성공"})
        # 구 리더 탈퇴
        st, body = api.call("DELETE", f"/bands/{b6['bandId']}/members/me", token=tok[str(b6["leaderMemberId"])], expect="any")
        print(f"   구 리더 탈퇴 -> {st}")
        scen.append({"case": "위임 후 구 리더 탈퇴", "memberId": b6["leaderMemberId"], "result": f"{st}"})
        if st < 300:
            b6["leaderMemberId"] = tgt.get("memberId")
except api.ApiError as e:
    print(f"   리더 위임 실패: {e}")
    scen.append({"case": "밴드 리더 위임", "result": f"실패 {e.status} {str(e.body)[:200]}"})

S["band6_scenarios"] = scen
S["fixupApiSummary"] = api.summary()
json.dump(S, open(STATE_PATH, "w"), ensure_ascii=False, indent=1)
print(f"\n포스터 총 {len(S['posters'])}개 / 공연 {len(S['performances'])}개")
print("API", api.summary()["total"], "건, 실패", api.summary()["failed"])

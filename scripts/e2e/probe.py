"""선곡 → 세션지원 → 확정 → lock → 셋리스트 승격 → 공연 파이프라인 축소 검증.

대량 시드 전에 경로 전체가 뚫리는지 확인한다. 실패는 그대로 장애 후보.
"""

import json
import sys

import api

OUT = []


def step(name, fn):
    try:
        r = fn()
        OUT.append({"step": name, "ok": True, "result": str(r)[:200]})
        print(f"  OK   {name}: {str(r)[:160]}")
        return r
    except Exception as e:
        OUT.append({"step": name, "ok": False, "error": str(e)[:400]})
        print(f"  FAIL {name}: {str(e)[:300]}")
        return None


sfx = sys.argv[1] if len(sys.argv) > 1 else "p1"
print("== 계정 준비 ==")
users = []
for i in range(1, 5):
    em = f"probe{sfx}_{i}@bandage.test"
    api.join(em, f"프로브{i}")
    users.append({"email": em, "token": api.login(em), "id": None})
for u in users:
    u["id"] = api.call("GET", "/members/me", token=u["token"])["memberId"]
print("  members:", [u["id"] for u in users])

lead = users[0]["token"]
band = step("밴드 생성", lambda: api.call("POST", "/bands", token=lead, body={"name": f"프로브밴드{sfx}", "description": "파이프라인 검증"}))
band_id = band["bandId"] if band else None

print("== 가입 신청/승인 ==")
for u in users[1:]:
    step(f"신청 m{u['id']}", lambda u=u: api.call("POST", f"/bands/{band_id}/applications", token=u["token"], body={}))
apps = step("신청 목록", lambda: api.call("GET", f"/bands/{band_id}/applications", token=lead, params={"pageSize": 50}))
if apps:
    for a in apps.get("content", []):
        aid = a.get("bandApplicationId") or a.get("id")
        step(f"승인 {aid}", lambda aid=aid: api.call("PATCH", f"/bands/{band_id}/applications/{aid}", token=lead, params={"status": "APPROVED"}))

print("== 선곡 생성 ==")
mids = [u["id"] for u in users]
sel = step(
    "선곡 생성",
    lambda: api.call("POST", "/track-selections", token=lead,
                     body={"title": f"프로브선곡{sfx}", "managerId": mids[0], "participantUserIds": mids, "bandIds": [band_id]}),
)
sel_id = api.pick_id(sel, "selectionId", "trackSelectionId", "id")
print("  selection id:", sel_id)

print("== 곡 추가 ==")
item = step(
    "항목 추가",
    lambda: api.call("POST", f"/track-selections/{sel_id}/items", token=lead,
                     body={"title": "Pull Me Under", "artist": "Dream Theater", "album": "Images and Words",
                           "duration": 494, "note": "프로브", "reference": "https://youtu.be/SGRgAULYgWE",
                           "sessions": [{"label": "VOCAL", "custom": False}, {"label": "GUITAR", "custom": False},
                                        {"label": "BASS", "custom": False}, {"label": "DRUM", "custom": False}]}),
)
item_id = api.pick_id(item, "itemId", "id")
print("  item id:", item_id, "raw:", json.dumps(item, ensure_ascii=False)[:400])

detail = step("항목 조회", lambda: api.call("GET", f"/track-selections/{sel_id}/items/{item_id}", token=lead))
print("  item detail:", json.dumps(detail, ensure_ascii=False)[:700])

print("== 선택/지원/확정 ==")
sess = (detail or {}).get("sessions") or []
print(f"  세션 {len(sess)}개 — 전 세션 확정 필요")
for i, sx in enumerate(sess):
    sid = sx.get("sessionId")
    u = users[i % len(users)]
    step(f"지원 {sx['label']} m{u['id']}",
         lambda u=u, sid=sid: api.call("POST", f"/track-selections/{sel_id}/items/{item_id}/sessions/{sid}/applicants", token=u["token"]))
    step(f"확정 {sx['label']}",
         lambda u=u, sid=sid: api.call("PATCH", f"/track-selections/{sel_id}/items/{item_id}/sessions/{sid}/confirmations",
                                       token=lead, body={"confirm": [u["id"]], "unconfirm": []}))

step("항목 선택", lambda: api.call("PATCH", f"/track-selections/{sel_id}/items/{item_id}/selection", token=lead, body={"selected": True}))
step("채팅 작성", lambda: api.call("POST", f"/track-selections/{sel_id}/items/{item_id}/chat", token=lead, body={"message": "프로브 코멘트"}))
step("채팅 조회", lambda: api.call("GET", f"/track-selections/{sel_id}/items/{item_id}/chat", token=lead, params={"pageSize": 10}))

print("== lock → 셋리스트 승격 ==")
step("lock", lambda: api.call("POST", f"/track-selections/{sel_id}/lock", token=lead, body={}))
setlist = step("셋리스트 생성", lambda: api.call("POST", "/setlists", token=lead, body={"trackSelectionId": sel_id, "title": f"프로브셋리스트{sfx}"}))
sl_id = api.pick_id(setlist, "setlistId", "id")
print("  setlist id:", sl_id, json.dumps(setlist, ensure_ascii=False)[:300])
step("셋리스트 트랙", lambda: api.call("GET", f"/setlists/{sl_id}/tracks", token=lead, params={"pageSize": 50}))
step("셋리스트 참여자", lambda: api.call("GET", f"/setlists/{sl_id}/participants", token=lead))

print("== 공연 ==")
perf = step(
    "공연 생성",
    lambda: api.call("POST", "/performances", token=lead,
                     body={"title": f"프로브공연{sfx}", "startAt": "2026-09-20 19:00", "durationMinutes": 120,
                           "venue": "홍대 롤링홀", "setlistIds": [sl_id] if sl_id else []}),
)
perf_id = api.pick_id(perf, "performanceId", "id")
print("  performance id:", perf_id, json.dumps(perf, ensure_ascii=False)[:300])
step("공연 조회", lambda: api.call("GET", f"/performances/{perf_id}", token=lead))
step("공연 셋리스트 트랙", lambda: api.call("GET", f"/performances/{perf_id}/setlists/tracks", token=lead))
step("포스터 presign", lambda: api.call("POST", "/performance-posters/presigned-url", token=lead, params={"performanceId": perf_id}, body={"contentLength": 1024, "contentType": "image/png", "ext": "png"}))

print("== 스케줄 보드 ==")
board = step("보드 생성", lambda: api.call("POST", f"/setlists/{sl_id}/schedule-boards", token=lead,
                                         body={"name": "프로브보드", "windowFrom": "2026-08-10", "windowTo": "2026-09-10",
                                               "constraints": {"excludeLateNight": True, "maxConsecutiveMinutes": 180, "workingHoursStart": 18, "workingHoursEnd": 44}}))
print("  board:", json.dumps(board, ensure_ascii=False)[:300])

print("== 가용시간 ==")
step("가용시간 등록", lambda: api.call("PUT", "/me/availability", token=lead,
                                   body={"effectiveFrom": "2026-08-10", "effectiveTo": "2026-09-10",
                                         "weeklyRules": [{"dayOfWeek": "MONDAY", "startSlot": 36, "endSlot": 44}],
                                         "exceptions": [{"date": "2026-08-15", "kind": "BLOCKED", "startSlot": 0, "endSlot": 47}],
                                         "note": "프로브"}))
step("가용시간 조회", lambda: api.call("GET", "/me/availability", token=lead))

print("\n== 요약 ==")
print(json.dumps(api.summary(), ensure_ascii=False, indent=1)[:2500])
json.dump({"steps": OUT, "ids": {"band": band_id, "selection": sel_id, "item": item_id, "setlist": sl_id, "performance": perf_id}},
          open(f"/tmp/probe_{sfx}.json", "w"), ensure_ascii=False, indent=1)

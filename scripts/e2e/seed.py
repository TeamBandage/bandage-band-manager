"""Bandage 개발 서버 테스트 데이터 시드.

probe.py 로 검증된 파이프라인을 규모대로 실행한다.
결과는 /tmp/bandage_seed_state.json 에 저장되어 verify.py 가 재사용한다.

실행: python3 seed.py [--bands N]
"""

import json
import os
import sys
import time
from concurrent.futures import ThreadPoolExecutor

import api
import spec

STATE_PATH = os.environ.get("SEED_STATE", "/tmp/bandage_seed_state.json")
RUN = os.environ.get("SEED_RUN", "r1")  # 이메일 네임스페이스 — 재실행 시 충돌 방지
POSTER_DIR = os.path.join(os.path.dirname(__file__), "../../poster-images")
ISSUES = []


def issue(where, detail, severity="MEDIUM"):
    ISSUES.append({"where": where, "detail": str(detail)[:500], "severity": severity})
    print(f"    !! [{severity}] {where}: {str(detail)[:200]}")


def email_of(idx):
    return f"member{idx}.{RUN}@bandage.test" if RUN != "r1" else f"member{idx}@bandage.test"


def confirm_all_sessions(tok_by_id, sel_id, item, participants, r):
    """항목의 모든 세션에 지원+확정. 서버는 전 세션 확정 전엔 selected 를 거부한다."""
    manager_tok = tok_by_id[participants[0]]
    for i, s in enumerate(item.get("sessions", [])):
        sid = s["sessionId"]
        who = participants[i % len(participants)]
        try:
            api.call("POST", f"/track-selections/{sel_id}/items/{item['trackSelectionItemId']}/sessions/{sid}/applicants",
                     token=tok_by_id[who])
        except api.ApiError as e:
            issue("세션 지원", e)
            continue
        try:
            api.call("PATCH", f"/track-selections/{sel_id}/items/{item['trackSelectionItemId']}/sessions/{sid}/confirmations",
                     token=manager_tok, body={"confirm": [who], "unconfirm": []})
        except api.ApiError as e:
            issue("세션 확정", e)


def main():
    t0 = time.time()
    r = spec.rng()
    plan = spec.build_plan()
    state = {"run": RUN, "members": {}, "bands": [], "selections": [], "setlists": [], "performances": [], "posters": []}

    # ---------- 1. 회원 ----------
    print(f"== 1. 회원 {plan['total_members']}명 ==")
    tok = {}

    def mk_member(idx):
        em = email_of(idx)
        api.join(em, spec.member_name(idx), spec.PASSWORD)
        t = api.login(em, spec.PASSWORD)
        mid = api.call("GET", "/members/me", token=t)["memberId"]
        return idx, em, t, mid

    with ThreadPoolExecutor(max_workers=8) as ex:
        for idx, em, t, mid in ex.map(mk_member, range(1, plan["total_members"] + 1)):
            tok[mid] = t
            state["members"][str(idx)] = {"loginId": spec.member_login_id(idx), "email": em, "memberId": mid,
                                          "name": spec.member_name(idx)}
    idx2mid = {int(k): v["memberId"] for k, v in state["members"].items()}
    print(f"   회원 {len(tok)}명 완료 ({time.time()-t0:.0f}s)")

    # ---------- 2. 밴드 + 가입 ----------
    print("== 2. 밴드 6개 + 멤버 가입 ==")
    for b in plan["bands"]:
        leader_mid = idx2mid[b["leader"]]
        lt = tok[leader_mid]
        band = api.call("POST", "/bands", token=lt, body={"name": b["name"], "description": b["description"]})
        bid = band["bandId"]
        b["band_id"] = bid
        b["leader_mid"] = leader_mid

        joiners = [idx2mid[i] for i in b["members"] if i != b["leader"]]
        joiners += [idx2mid[c["member"]] for c in plan["bands"][b["no"] - 1]["cross_joiners"] if False]  # noop
        for mid in joiners:
            try:
                api.call("POST", f"/bands/{bid}/applications", token=tok[mid])
            except api.ApiError as e:
                issue(f"{b['name']} 가입신청", e)
        # 승인
        apps = api.call("GET", f"/bands/{bid}/applications", token=lt, params={"pageSize": 100})
        for a in apps.get("content", []):
            try:
                api.call("PATCH", f"/bands/{bid}/applications/{a['bandApplicationId']}", token=lt,
                         params={"status": "APPROVED"})
            except api.ApiError as e:
                issue(f"{b['name']} 승인", e)
        state["bands"].append({"no": b["no"], "name": b["name"], "bandId": bid, "leaderMemberId": leader_mid,
                               "memberIds": [idx2mid[i] for i in b["members"]]})
        print(f"   {b['name']} ({bid[:8]}) 멤버 {len(joiners)+1}명")

    # 교차 가입 (각 밴드 2명이 타 밴드에 추가 가입)
    print("== 2-1. 교차 가입 ==")
    band_by_no = {x["no"]: x for x in state["bands"]}
    for b in plan["bands"]:
        for c in b["cross_joiners"]:
            mid = idx2mid[c["member"]]
            tb = band_by_no[c["target_band"]]
            try:
                api.call("POST", f"/bands/{tb['bandId']}/applications", token=tok[mid])
                apps = api.call("GET", f"/bands/{tb['bandId']}/applications", token=tok[tb["leaderMemberId"]],
                                params={"pageSize": 100})
                for a in apps.get("content", []):
                    if a["memberId"] == mid and a["status"] == "PENDING":
                        api.call("PATCH", f"/bands/{tb['bandId']}/applications/{a['bandApplicationId']}",
                                 token=tok[tb["leaderMemberId"]], params={"status": "APPROVED"})
                        tb.setdefault("crossMemberIds", []).append(mid)
            except api.ApiError as e:
                issue("교차 가입", e)
    print(f"   교차 가입 완료 ({time.time()-t0:.0f}s)")

    # ---------- 3. 밴드 이미지 ----------
    print("== 3. 밴드 프로필 이미지 ==")
    imgs = sorted(f for f in os.listdir(POSTER_DIR) if f.lower().endswith((".jpg", ".png")))
    for i, sb in enumerate(state["bands"]):
        f = os.path.join(POSTER_DIR, imgs[i % len(imgs)])
        ext = f.rsplit(".", 1)[-1].lower()
        try:
            pre = api.call("POST", f"/bands/{sb['bandId']}/profile-image/presigned-url", token=tok[sb["leaderMemberId"]],
                           body={"contentLength": os.path.getsize(f), "contentType": f"image/{'png' if ext=='png' else 'jpeg'}", "ext": ext})
            pre["contentType"] = f"image/{'png' if ext=='png' else 'jpeg'}"
            key = api.upload_image(pre, f)
            api.call("PATCH", f"/bands/{sb['bandId']}", token=tok[sb["leaderMemberId"]], body={"profileImg": key})
            sb["profileImg"] = key
        except Exception as e:
            issue(f"{sb['name']} 프로필 이미지", e)
    print(f"   이미지 완료 ({time.time()-t0:.0f}s)")

    # ---------- 4. 선곡 → 셋리스트 ----------
    print(f"== 4. 선곡/셋리스트 (밴드당 {spec.SETLISTS_PER_BAND}개 × {spec.TRACKS_PER_SETLIST}곡) ==")
    for sb in state["bands"]:
        members = sb["memberIds"]
        for sn in range(1, spec.SETLISTS_PER_BAND + 1):
            title = f"{sb['name']} 셋리스트 {sn}"
            # 절반은 밴드 전체, 절반은 개인자격 멤버 포함
            band_only = sn <= spec.SETLISTS_PER_BAND // 2
            if band_only:
                parts = members[:6]
                band_ids = [sb["bandId"]]
            else:
                outsiders = [m for ob in state["bands"] if ob["no"] != sb["no"] for m in ob["memberIds"][:2]]
                parts = members[:5] + r.sample(outsiders, 2)
                band_ids = [sb["bandId"]]
            mgr = parts[0]
            try:
                sel = api.call("POST", "/track-selections", token=tok[mgr],
                               body={"title": f"{title} 선곡", "managerId": mgr,
                                     "participantUserIds": parts, "bandIds": band_ids})
            except api.ApiError as e:
                issue(f"{title} 선곡생성", e, "HIGH")
                continue
            sel_id = sel["selectionId"]
            tracks = spec.tracks_for_setlist(sb["no"], sn, r)
            item_ok = 0
            for (ti, ta, tal, tdur) in tracks:
                try:
                    it = api.call("POST", f"/track-selections/{sel_id}/items", token=tok[r.choice(parts)],
                                  body={"title": ti, "artist": ta, "album": tal, "duration": tdur,
                                        "note": f"{ta} 곡", "reference": f"https://www.youtube.com/results?search_query={ta} {ti}",
                                        "sessions": spec.sessions_for(ti, r)})
                except api.ApiError as e:
                    issue(f"{title} 곡추가 {ti}", e)
                    continue
                confirm_all_sessions(tok, sel_id, it, parts, r)
                try:
                    api.call("PATCH", f"/track-selections/{sel_id}/items/{it['trackSelectionItemId']}/selection",
                             token=tok[mgr], body={"selected": True})
                    item_ok += 1
                except api.ApiError as e:
                    issue(f"{title} 선택 {ti}", e)
                if r.random() < 0.3:
                    try:
                        api.call("POST", f"/track-selections/{sel_id}/items/{it['trackSelectionItemId']}/chat",
                                 token=tok[r.choice(parts)], body={"message": f"{ti} 좋아요"})
                    except api.ApiError as e:
                        issue("채팅", e)
            state["selections"].append({"selectionId": sel_id, "band": sb["no"], "title": title,
                                        "managerId": mgr, "participants": parts, "selectedTracks": item_ok})
            try:
                api.call("POST", f"/track-selections/{sel_id}/lock", token=tok[mgr])
                sl = api.call("POST", "/setlists", token=tok[mgr], body={"trackSelectionId": sel_id, "title": title})
                state["setlists"].append({"setlistId": sl["setlistId"], "band": sb["no"], "title": title,
                                          "managerId": mgr, "participants": parts, "bandOnly": band_only,
                                          "trackCount": item_ok})
            except api.ApiError as e:
                issue(f"{title} 승격", e, "HIGH")
        print(f"   {sb['name']} 셋리스트 완료 ({time.time()-t0:.0f}s)")

    # ---------- 5. 공연 ----------
    print("== 5. 공연 (밴드당 3개: 단독/2밴드/3밴드 연합) ==")
    for sb in state["bands"]:
        own = [s for s in state["setlists"] if s["band"] == sb["no"]]
        for pn in range(1, spec.PERFORMANCES_PER_BAND + 1):
            others = [o for o in state["bands"] if o["no"] != sb["no"]]
            partner_bands = [] if pn == 1 else r.sample(others, pn - 1)
            # 공연 생성자는 자기 셋리스트만 넣을 수 있다(SETLIST_FORBIDDEN).
            # 연합 셋리스트는 초대 수락 후 상대 매니저가 batch 로 추가한다.
            sl_ids = [own[(pn - 1) % len(own)]["setlistId"]] if own else []
            partner_setlists = []
            for pb in partner_bands:
                ps = [s for s in state["setlists"] if s["band"] == pb["no"]]
                if ps:
                    partner_setlists.append(ps[0])
            kind = ["단독", "2밴드 연합", "3밴드 연합"][pn - 1]
            title = f"{sb['name']} 공연 {pn} ({kind})"
            day = 10 + (sb["no"] * 3 + pn) % 20
            try:
                perf = api.call("POST", "/performances", token=tok[sb["leaderMemberId"]],
                                body={"title": title, "startAt": f"2026-09-{day:02d} 19:00",
                                      "durationMinutes": 90 + pn * 30, "venue": spec.VENUES[(sb["no"] + pn) % len(spec.VENUES)],
                                      "setlistIds": sl_ids})
            except api.ApiError as e:
                issue(f"{title} 생성", e, "HIGH")
                continue
            pid = perf["performanceId"]
            # 연합 공연이면 상대 밴드 리더를 매니저로 초대
            for pb in partner_bands:
                try:
                    inv = api.call("POST", f"/performances/{pid}/invitations", token=tok[sb["leaderMemberId"]],
                                   body={"memberId": pb["leaderMemberId"]})
                    iid = api.pick_id(inv, "invitationId", "id")
                    api.call("PATCH", f"/performances/{pid}/invitations/{iid}", token=tok[pb["leaderMemberId"]],
                             params={"status": "ACCEPTED"})
                except api.ApiError as e:
                    issue(f"{title} 초대", e)
            # 초대 수락한 상대 매니저가 자기 셋리스트를 공연에 추가
            for ps in partner_setlists:
                try:
                    api.call("POST", f"/performances/{pid}/setlists/batch", token=tok[ps["managerId"]],
                             body={"setlistIds": [ps["setlistId"]]})
                    sl_ids.append(ps["setlistId"])
                except api.ApiError as e:
                    issue(f"{title} 연합 셋리스트 추가", e)
            state["performances"].append({"performanceId": pid, "band": sb["no"], "title": title, "kind": kind,
                                          "setlistIds": sl_ids, "ownerMemberId": sb["leaderMemberId"],
                                          "partnerBands": [p["no"] for p in partner_bands]})
        print(f"   {sb['name']} 공연 3개 ({time.time()-t0:.0f}s)")

    # ---------- 6. 포스터 ----------
    print("== 6. 공연 포스터 (poster-images 전량 사용) ==")
    for i, p in enumerate(state["performances"]):
        f = os.path.join(POSTER_DIR, imgs[i % len(imgs)])
        ext = f.rsplit(".", 1)[-1].lower()
        ct = "image/png" if ext == "png" else "image/jpeg"
        try:
            pre = api.call("POST", "/performance-posters/presigned-url", token=tok[p["ownerMemberId"]],
                           params={"performanceId": p["performanceId"]},
                           body={"contentLength": os.path.getsize(f), "contentType": ct, "ext": ext})
            pre["contentType"] = ct
            key = api.upload_image(pre, f)
            po = api.call("POST", "/performance-posters", token=tok[p["ownerMemberId"]],
                          body={"performanceId": p["performanceId"], "imageKey": key,
                                "description": f"{p['title']} 포스터"})
            state["posters"].append({"posterId": api.pick_id(po, "posterId", "id"),
                                     "performanceId": p["performanceId"], "image": os.path.basename(f)})
        except Exception as e:
            issue(f"{p['title']} 포스터", e, "HIGH")
    print(f"   포스터 {len(state['posters'])}개 ({time.time()-t0:.0f}s)")

    # ---------- 7. 스케줄(가용시간) ----------
    print("== 7. 멤버 가용시간 (일반 80% / 특이 20%) ==")
    days = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]
    sched_n = 0
    for k, m in state["members"].items():
        mid = m["memberId"]
        if r.random() < 0.8:  # 일반: 평일 저녁 + 주말 오후
            rules = [{"dayOfWeek": d, "startSlot": 36, "endSlot": 44} for d in days[:5]]
            rules += [{"dayOfWeek": d, "startSlot": 26, "endSlot": 40} for d in days[5:]]
            exc = [{"date": "2026-08-15", "kind": "BLOCKED", "startSlot": 0, "endSlot": 47}]
            kind = "일반"
        else:  # 특이: 심야/새벽, 단일 요일, 종일 가용 등
            pick = r.randint(0, 2)
            if pick == 0:
                rules = [{"dayOfWeek": d, "startSlot": 44, "endSlot": 47} for d in days]
                kind = "심야만"
            elif pick == 1:
                rules = [{"dayOfWeek": days[r.randint(0, 6)], "startSlot": 0, "endSlot": 47}]
                kind = "단일요일 종일"
            else:
                rules = []
                kind = "가용시간 없음"
            exc = [{"date": f"2026-08-{r.randint(10,28):02d}", "kind": "AVAILABLE", "startSlot": 20, "endSlot": 30},
                   {"date": f"2026-09-{r.randint(1,10):02d}", "kind": "BLOCKED", "startSlot": 0, "endSlot": 47}]
        try:
            api.call("PUT", "/me/availability", token=tok[mid],
                     body={"effectiveFrom": "2026-08-10", "effectiveTo": "2026-09-10",
                           "weeklyRules": rules, "exceptions": exc, "note": f"{m['name']} {kind}"})
            sched_n += 1
        except api.ApiError as e:
            issue(f"가용시간 {m['name']}", e)
    print(f"   가용시간 {sched_n}명 ({time.time()-t0:.0f}s)")

    # ---------- 8. 스케줄 보드 ----------
    print("== 8. 셋리스트별 스케줄 보드 ==")
    board_n = 0
    for s in state["setlists"][:12]:  # 대표 12개
        try:
            bd = api.call("POST", f"/setlists/{s['setlistId']}/schedule-boards", token=tok[s["managerId"]],
                          body={"name": f"{s['title']} 보드", "windowFrom": "2026-08-10", "windowTo": "2026-09-10",
                                "constraints": {"excludeLateNight": True, "maxConsecutiveMinutes": 180,
                                                "workingHoursStart": 18, "workingHoursEnd": 44}})
            s["boardId"] = api.pick_id(bd, "boardId", "id")
            board_n += 1
        except api.ApiError as e:
            issue(f"보드 {s['title']}", e)
    print(f"   보드 {board_n}개")

    # ---------- 9. 밴드6 탈퇴 시나리오 ----------
    print("== 9. 밴드 6 탈퇴/제명 시나리오 ==")
    b6 = band_by_no.get(max(band_by_no))  # 마지막 밴드 대상
    lt6 = tok[b6["leaderMemberId"]]
    scen = []
    ms = [m for m in b6["memberIds"] if m != b6["leaderMemberId"]]
    # (a) 자진 탈퇴
    for mid in ms[:3]:
        try:
            api.call("DELETE", f"/bands/{b6['bandId']}/members/me", token=tok[mid])
            scen.append({"case": "자진 탈퇴", "memberId": mid, "result": "성공"})
        except api.ApiError as e:
            scen.append({"case": "자진 탈퇴", "memberId": mid, "result": f"실패 {e.status}"})
            issue("밴드6 자진탈퇴", e)
    # (b) 리더의 제명
    try:
        bms = api.call("GET", f"/bands/{b6['bandId']}/members", token=lt6, params={"pageSize": 100})
        cand = [x for x in bms.get("content", []) if x.get("memberId") in ms[3:6]]
        for c in cand:
            bmid = api.pick_id(c, "bandMemberId", "id")
            try:
                api.call("DELETE", f"/bands/{b6['bandId']}/members/{bmid}", token=lt6)
                scen.append({"case": "리더 제명", "memberId": c.get("memberId"), "result": "성공"})
            except api.ApiError as e:
                scen.append({"case": "리더 제명", "memberId": c.get("memberId"), "result": f"실패 {e.status}"})
                issue("밴드6 제명", e)
    except api.ApiError as e:
        issue("밴드6 멤버조회", e)
    # (c) 셋리스트 매니저 위임 후 이탈
    s6 = [s for s in state["setlists"] if s["band"] == b6["no"]]
    if s6:
        s = s6[0]
        newm = [p for p in s["participants"] if p != s["managerId"]]
        if newm:
            try:
                api.call("PATCH", f"/setlists/{s['setlistId']}/manager", token=tok[s["managerId"]],
                         body={"newManagerId": newm[0]})
                scen.append({"case": "셋리스트 매니저 위임", "from": s["managerId"], "to": newm[0], "result": "성공"})
            except api.ApiError as e:
                scen.append({"case": "셋리스트 매니저 위임", "result": f"실패 {e.status}"})
                issue("셋리스트 매니저 위임", e)
    # (d) 연합공연 오너 위임
    p6 = [p for p in state["performances"] if p["band"] == b6["no"] and p["partnerBands"]]
    if p6:
        p = p6[0]
        target = band_by_no[p["partnerBands"][0]]["leaderMemberId"]
        try:
            api.call("PATCH", f"/performances/{p['performanceId']}/owner", token=tok[p["ownerMemberId"]],
                     body={"newOwnerId": target})
            scen.append({"case": "연합공연 오너 위임", "to": target, "result": "성공"})
        except api.ApiError as e:
            scen.append({"case": "연합공연 오너 위임", "result": f"실패 {e.status}"})
            issue("공연 오너 위임", e)
    state["band6_scenarios"] = scen
    for x in scen:
        print(f"   {x['case']}: {x['result']}")

    # ---------- 저장 ----------
    state["tokens"] = tok
    state["issues"] = ISSUES
    state["apiSummary"] = api.summary()
    json.dump(state, open(STATE_PATH, "w"), ensure_ascii=False, indent=1)
    print(f"\n== 완료 {time.time()-t0:.0f}s ==")
    print(f"밴드 {len(state['bands'])} / 셋리스트 {len(state['setlists'])} / 공연 {len(state['performances'])} / 포스터 {len(state['posters'])}")
    print(f"API 호출 {api.summary()['total']}건, 실패 {api.summary()['failed']}건, 이슈 {len(ISSUES)}건")
    print("state:", STATE_PATH)


if __name__ == "__main__":
    main()

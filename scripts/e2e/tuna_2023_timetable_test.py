"""TuNA X SORIHANA 2023 겨울 연합공연 시간표 자동배치 재현 테스트 (2차 검증).

원본: 구글 시트 (docs.google.com/spreadsheets/d/1ip0tXvlonXcSWyJtDXtwVe29Wd8CQ9U7RceSm45Wy7k)
- '3. 개인 시간표' (gid=620372208): 멤버 33명 요일별 가용시간
- '1. 선곡회의' / '4. 확정':        확정 24곡의 세션 배정
- '2. 합주 시간표':                 사람이 손으로 짠 시간표 (비교 기준)

1차 검증([[tuna_timetable_test.py]], TuNA 2020)과 같은 알고리즘을 **다른 규모의 데이터셋**으로
재검증한다. 2020년 대비: 멤버 30→33명, 곡 23→24곡, 단일룸→2룸 병렬 운용.

실행 절차:
  1) docker compose -f docker-compose-test.yml up -d
  2) set -a; source .env; set +a; ./gradlew bootRun
  3) python3 scripts/e2e/tuna_2023_timetable_test.py

가정(시트에 없거나 불확실해서 정한 것):
- '미정'/'?' 로 표기된 가용성은 **불가**로 처리한다 (심창연 월금토일, 허식 일, 유탁영 평일).
- 지범준은 전 요일 공란(12월 말 확정 예정) → 제약 없음(전 요일 종일 가용)으로 처리하고 학회 일정만 차단.
- 설 연휴(1/21~1/24)는 시트에 멤버별 차단이 없어 별도 반영하지 않았다.
- 'Alter bridge - Isolation' 은 G1/G2 모두 나경훈이라 멤버 집합에서 중복 제거했다.
- 곡당 30분(jamDurationSlots=1). 원본 시간표가 대체로 30분 단위다.
"""

import datetime
import os
from collections import Counter

os.environ.setdefault("BANDAGE_BASE", "http://localhost:8080/api/v1")
import api  # noqa: E402

PW = "12345678"
WINDOW = ("2023-01-09", "2023-02-12")  # 원본 시간표 1주차 월 ~ 5주차 일
DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]
WEEKDAYS = DAYS[:5]
FULL = (0, 48)

# ---------------------------------------------------------------- 멤버 & 가용성
# 슬롯 = 30분. 16=08:00, 24=12:00, 30=15:00, 34=17:00, 36=18:00, 38=19:00, 40=20:00, 44=22:00
def wd(rng):
    """평일 전체에 같은 구간 적용."""
    return {d: [rng] for d in WEEKDAYS}


MEMBERS = [
    ("나유빈", {**wd((38, 48)), "SATURDAY": [FULL], "SUNDAY": [FULL]}, []),
    ("김형건", wd((38, 48)), []),  # 토·일 불가
    ("이건휘", {"TUESDAY": [FULL], "SATURDAY": [FULL], "SUNDAY": [FULL]}, []),
    # 알바 미정 — 화·수·목만 확정
    ("심창연", {"TUESDAY": [FULL], "WEDNESDAY": [FULL], "THURSDAY": [FULL]}, []),
    ("나수현", {"MONDAY": [(40, 48)], "TUESDAY": [(40, 48)], "WEDNESDAY": [(40, 48)],
                "THURSDAY": [(40, 48)], "FRIDAY": [(16, 48)], "SATURDAY": [FULL], "SUNDAY": [FULL]}, []),
    ("김수민", {"MONDAY": [(40, 48)], "TUESDAY": [(40, 48)], "WEDNESDAY": [(40, 48)],
                "THURSDAY": [(40, 48)], "FRIDAY": [(37, 48)], "SATURDAY": [(37, 48)], "SUNDAY": [FULL]}, []),
    # 일요일 불확실(촬영) → 불가
    ("허식", {**wd(FULL), "SATURDAY": [FULL]}, []),
    ("안성진", {**wd((34, 48)), "SATURDAY": [FULL], "SUNDAY": [FULL]}, []),
    ("정선우", {**wd((40, 48)), "SATURDAY": [FULL], "SUNDAY": [FULL]},
     [("2023-01-30", "BLOCKED", None, None)]),
    ("이지우", {"TUESDAY": [(0, 30)], "WEDNESDAY": [(0, 30)], "FRIDAY": [(0, 30)], "SATURDAY": [FULL]}, []),
    ("정도윤", {"MONDAY": [FULL], "TUESDAY": [FULL], "WEDNESDAY": [(0, 36)], "THURSDAY": [(0, 36)],
                "FRIDAY": [(0, 36)], "SATURDAY": [FULL], "SUNDAY": [FULL]}, []),
    ("정재희", {"MONDAY": [(30, 48)], "TUESDAY": [(30, 42)], "WEDNESDAY": [(0, 36)],
                "THURSDAY": [(30, 48)], "FRIDAY": [(0, 42)]}, []),
    ("박지민", {**wd(FULL), "SATURDAY": [(34, 48)], "SUNDAY": [FULL]}, []),
    ("나경훈", {"TUESDAY": [(28, 48)], "WEDNESDAY": [(0, 28)], "THURSDAY": [(28, 48)],
                "FRIDAY": [(0, 28)], "SATURDAY": [FULL], "SUNDAY": [FULL]}, []),
    ("장대원", {d: [FULL] for d in DAYS}, []),
    # 전 요일 공란(확정 예정) → 제약 없음으로 간주. 학회 일정만 예외로 반영
    # 빈 주간규칙으로 등록하면 "항상 불가"가 되므로 전 요일 FULL 로 등록해야 한다
    ("지범준", {d: [FULL] for d in DAYS},
     [("2023-01-16", "BLOCKED", None, None), ("2023-01-17", "BLOCKED", None, None),
      ("2023-01-18", "BLOCKED", None, None)]),
    ("이재아", {"MONDAY": [FULL], "TUESDAY": [FULL], "WEDNESDAY": [(0, 24)], "THURSDAY": [(0, 24)],
                "FRIDAY": [(0, 37)], "SATURDAY": [FULL], "SUNDAY": [FULL]}, []),
    ("최유진", {"MONDAY": [FULL], "TUESDAY": [FULL], "WEDNESDAY": [FULL], "THURSDAY": [FULL],
                "FRIDAY": [(39, 48)], "SATURDAY": [FULL], "SUNDAY": [FULL]}, []),
    ("정도헌", {**wd((38, 48)), "SATURDAY": [FULL], "SUNDAY": [FULL]}, []),
    ("진시현", {"MONDAY": [(30, 48)], "TUESDAY": [FULL], "WEDNESDAY": [(30, 48)], "THURSDAY": [FULL],
                "FRIDAY": [(28, 48)], "SATURDAY": [FULL], "SUNDAY": [FULL]}, []),
    ("송민지", {"MONDAY": [FULL], "TUESDAY": [(40, 48)], "WEDNESDAY": [(40, 48)],
                "THURSDAY": [(40, 48)], "FRIDAY": [FULL], "SATURDAY": [FULL]},
     [("2023-02-10", "BLOCKED", None, None)]),
    ("윤상우", {**wd((36, 48)), "SATURDAY": [FULL]},
     [("2023-01-24", "BLOCKED", None, None), ("2023-01-25", "BLOCKED", None, None),
      ("2023-01-26", "BLOCKED", None, None), ("2023-01-27", "BLOCKED", None, None),
      ("2023-02-10", "BLOCKED", None, None)]),
    ("곽주은", {**wd((32, 48)), "SATURDAY": [FULL], "SUNDAY": [FULL]},
     [(d, "BLOCKED", None, None) for d in
      ("2023-01-15", "2023-01-16", "2023-01-17", "2023-01-18", "2023-01-27", "2023-01-28",
       "2023-01-30", "2023-02-03", "2023-02-07", "2023-02-10")]
     + [("2023-01-31", "BLOCKED", 0, 24)]),
    ("신선경", {"MONDAY": [(39, 48)], "TUESDAY": [(39, 48)], "WEDNESDAY": [(39, 48)],
                "THURSDAY": [(39, 48)], "FRIDAY": [(35, 48)], "SATURDAY": [FULL], "SUNDAY": [FULL]}, []),
    ("서재운", {"TUESDAY": [FULL], "WEDNESDAY": [FULL], "THURSDAY": [(0, 30)], "FRIDAY": [(0, 30)],
                "SATURDAY": [(0, 30)], "SUNDAY": [(0, 30)]}, []),
    ("이동후", {"MONDAY": [(36, 48)], "WEDNESDAY": [(36, 48)], "THURSDAY": [FULL],
                "SUNDAY": [(36, 48)]}, []),
    ("강나리", {**wd((39, 48)), "SATURDAY": [FULL], "SUNDAY": [FULL]}, []),
    ("최홍석", {"MONDAY": [(30, 48)], "TUESDAY": [FULL], "WEDNESDAY": [(30, 48)], "THURSDAY": [FULL],
                "FRIDAY": [(30, 48)], "SATURDAY": [(32, 48)], "SUNDAY": [(36, 48)]}, []),
    # 평일 전부 '?' → 불가. 주말만 확정
    ("유탁영", {"SATURDAY": [FULL], "SUNDAY": [FULL]},
     [("2023-01-29", "BLOCKED", None, None)]),
    ("허영주", {d: [FULL] for d in DAYS},
     [(d, "BLOCKED", None, None) for d in
      ("2023-01-13", "2023-01-14", "2023-01-15", "2023-01-16", "2023-01-17",
       "2023-01-30", "2023-01-31", "2023-02-01", "2023-02-02", "2023-02-03")]),
    ("안태인", {d: [FULL] for d in DAYS}, []),
]

# ---------------------------------------------------------------- 확정 24곡
# 역할 -> 멤버들 ('x' 세션은 생략)
SONGS = [
    ("실리카겔", "NO PAIN", {"V": ["허식"], "GA": ["정선우"], "GB": ["정도윤"], "B": ["신선경"], "D": ["유탁영"]}),
    ("실리카겔", "Neo Soul", {"V": ["허식"], "GA": ["정도윤"], "GB": ["최유진"], "B": ["신선경"], "D": ["허영주"]}),
    ("Arctic Monkeys", "R U Mine", {"V": ["허식"], "GA": ["정선우"], "GB": ["이재아"], "B": ["신선경"], "D": ["허영주"]}),
    ("국카스텐", "Vitriol", {"V": ["안성진"], "GA": ["나경훈"], "GB": ["이재아"], "B": ["윤상우"], "D": ["강나리"]}),
    ("trivium", "In Waves", {"V": ["안성진"], "GA": ["나경훈"], "GB": ["정선우"], "B": ["신선경"], "D": ["이동후"]}),
    # G1=G2=나경훈 → 멤버 집합에서 중복 제거
    ("Alter bridge", "Isolation", {"V": ["안성진"], "GA": ["나경훈"], "B": ["정선우"], "D": ["이동후"]}),
    ("Surl", "Cilla", {"V": ["김형건"], "GA": ["정선우"], "B": ["심창연"], "D": ["서재운"]}),
    ("크러쉬", "Intro + Cereal", {"V": ["김형건"], "GA": ["장대원"], "B": ["심창연"], "D": ["서재운"]}),
    ("혁오", "Mer", {"V": ["김형건"], "GA": ["정선우"], "GB": ["최유진"], "B": ["곽주은"], "D": ["유탁영"]}),
    ("김뜻돌", "비 오는 거리에서 춤을 추자", {"V": ["이건휘"], "GA": ["정도윤"], "B": ["곽주은"], "D": ["허영주"]}),
    ("차세대", "악광무", {"V": ["이건휘"], "GA": ["정재희"], "B": ["송민지"], "D": ["강나리"]}),
    ("검정치마", "Electra", {"V": ["이건휘"], "GA": ["이재아"], "GB": ["박지민"], "B": ["송민지"],
                             "D": ["최홍석"], "KA": ["신선경"]}),
    ("페퍼톤스", "러브앤피스", {"V": ["심창연"], "GA": ["정선우"], "GB": ["정재희"], "B": ["송민지"],
                                "D": ["이동후"], "KA": ["신선경"]}),
    ("언니네 이발관", "아름다운 것", {"V": ["심창연"], "GA": ["김형건"], "B": ["송민지"], "D": ["서재운"],
                                     "KA": ["정선우"]}),
    ("언니네 이발관", "나를 잊었나요", {"V": ["심창연"], "GA": ["김형건"], "B": ["송민지"], "D": ["서재운"],
                                       "KA": ["정선우"]}),
    ("윤하", "사건의 지평선", {"V": ["나유빈"], "GA": ["정도헌"], "GB": ["진시현"], "B": ["윤상우"],
                               "D": ["최홍석"], "KA": ["신선경"]}),
    ("아이유", "라일락", {"V": ["나유빈"], "GA": ["정도헌"], "GB": ["장대원"], "B": ["윤상우"],
                          "D": ["유탁영"], "KA": ["안태인"]}),
    ("체리필터", "Happy Day", {"V": ["나유빈"], "GA": ["장대원"], "GB": ["진시현"], "B": ["심창연"],
                               "D": ["강나리"], "KA": ["안태인"]}),
    ("윤하", "26.0", {"V": ["김수민"], "GA": ["지범준"], "GB": ["정도헌"], "B": ["정선우"],
                      "D": ["강나리"], "KB": ["신선경"]}),
    ("백예린", "물고기", {"V": ["김수민"], "GA": ["진시현"], "GB": ["최유진"], "B": ["곽주은"],
                          "D": ["최홍석"], "KA": ["안태인"]}),
    ("자우림", "스물다섯 스물하나", {"V": ["김수민"], "GA": ["진시현"], "GB": ["박지민"], "B": ["윤상우"],
                                     "D": ["최홍석"], "KA": ["안태인"]}),
    ("쏜애플", "행복한 나를", {"V": ["나수현"], "GA": ["최유진"], "GB": ["진시현"], "B": ["곽주은"],
                               "D": ["허영주"]}),
    ("morden harket", "Cant Take My Eyes Off You", {"V": ["나수현"], "GA": ["진시현"], "B": ["윤상우"],
                                                    "D": ["유탁영"], "KA": ["안태인"]}),
    ("쏜애플", "한낮", {"V": ["나수현"], "GA": ["정재희"], "GB": ["이재아"], "B": ["정선우"], "D": ["최홍석"]}),
]

ROLE_LABEL = {"V": "VOCAL", "GA": "GUITARONE", "GB": "GUITARTWO", "B": "BASS", "D": "DRUM",
              "KA": "KEYSONE", "KB": "KEYSTWO"}

# ---------------------------------------------------------------- 사람이 짠 시간표 (정본)
# 4~5주차 정상 운영 주 기준. 곡 -> (요일, 시작 슬롯)
EXPECTED = {
    "사건의 지평선": ("SATURDAY", 34), "Happy Day": ("SATURDAY", 35), "라일락": ("SATURDAY", 36),
    "R U Mine": ("SATURDAY", 38), "NO PAIN": ("SATURDAY", 39),
    "26.0": ("SATURDAY", 40), "Neo Soul": ("SATURDAY", 41),
    "물고기": ("SATURDAY", 42), "행복한 나를": ("SATURDAY", 43),
    "Cant Take My Eyes Off You": ("SATURDAY", 38), "스물다섯 스물하나": ("SATURDAY", 39),
    "Intro + Cereal": ("TUESDAY", 38), "Vitriol": ("TUESDAY", 40), "나를 잊었나요": ("TUESDAY", 40),
    "한낮": ("TUESDAY", 41), "악광무": ("TUESDAY", 42), "Mer": ("TUESDAY", 42),
    "비 오는 거리에서 춤을 추자": ("TUESDAY", 43), "아름다운 것": ("TUESDAY", 44),
    "Electra": ("TUESDAY", 45),
    "러브앤피스": ("THURSDAY", 40), "Isolation": ("THURSDAY", 40), "In Waves": ("THURSDAY", 41),
    # Cilla 는 원본 시간표에 등장하지 않는다(확정 목록에만 존재)
}

AUTO_PLACE_REQUEST = {
    "interval": "WEEKLY",
    "jamDurationSlots": 1,           # 곡당 30분
    "maxJamsPerDay": 12,             # 원본은 하루 여러 모임 허용
    "maxEmptySlotsBetweenJams": 46,  # 모임 간 간격 제약 없음
    "dayPreference": ["SATURDAY", "TUESDAY", "THURSDAY", "SUNDAY", "FRIDAY", "WEDNESDAY", "MONDAY"],
    "startTimePreference": 26,       # 13:00 (원본 시간표 첫 행)
    "endTimePreference": 46,         # 23:00 (원본 시간표 마지막 행)
}


def slot_hhmm(s):
    return f"{s // 2:02d}:{(s % 2) * 30:02d}"


def main():
    print("== 1. 회원 ==")
    token, member_id = {}, {}
    for i, (name, _, _) in enumerate(MEMBERS, start=1):
        em = f"tuna23-{i:02d}@bandage.test"
        api.join(em, name, PW)
        t = api.login(em, PW)
        token[name] = t
        member_id[name] = api.call("GET", "/members/me", token=t)["memberId"]
    manager = "정선우"
    mgr_tok = token[manager]
    print(f"   {len(token)}명 로그인")

    print("== 2. 밴드 ==")
    band = api.call("POST", "/bands", token=mgr_tok,
                    body={"name": f"TuNA-SORIHANA-{os.getpid()}", "description": "2023 겨울 연합공연"})
    band_id = api.pick_id(band, "bandId")
    for name in token:
        if name != manager:
            api.call("POST", f"/bands/{band_id}/applications", token=token[name], expect=[200, 409])
    apps = api.call("GET", f"/bands/{band_id}/applications", token=mgr_tok, params={"pageSize": 100})
    for a in apps.get("content", apps if isinstance(apps, list) else []):
        if a.get("status") == "PENDING":
            api.call("PATCH", f"/bands/{band_id}/applications/{a['bandApplicationId']}", token=mgr_tok,
                     body={"status": "APPROVED"}, expect=[200, 400, 409])
    print(f"   bandId={band_id}")

    print("== 3. 선곡 ==")
    sel = api.call("POST", "/track-selections", token=mgr_tok,
                   body={"title": "2023 겨울 연합공연 선곡", "managerId": member_id[manager],
                         "participantUserIds": list(member_id.values()), "bandIds": [band_id]})
    sel_id = api.pick_id(sel, "selectionId")
    for artist, title, roles in SONGS:
        # 세션 정원 1명 → 더블 캐스팅은 멤버마다 세션 분리
        slots = [(ROLE_LABEL[r] + ("" if i == 0 else "B"), r, nm)
                 for r, names in roles.items() for i, nm in enumerate(names)]
        item = api.call("POST", f"/track-selections/{sel_id}/items", token=mgr_tok,
                        body={"title": title, "artist": artist, "duration": 240,
                              "sessions": [{"label": lb, "custom": r in ("KA", "KB")} for lb, r, _ in slots]})
        item_id = item["trackSelectionItemId"]
        by_label = {s["label"]: s["sessionId"] for s in item["sessions"]}
        for lb, _, nm in slots:
            sid = by_label[lb]
            api.call("POST", f"/track-selections/{sel_id}/items/{item_id}/sessions/{sid}/applicants",
                     token=token[nm])
            api.call("PATCH", f"/track-selections/{sel_id}/items/{item_id}/sessions/{sid}/confirmations",
                     token=mgr_tok, body={"confirm": [member_id[nm]], "unconfirm": []})
        api.call("PATCH", f"/track-selections/{sel_id}/items/{item_id}/selection", token=mgr_tok,
                 body={"selected": True})
    api.call("POST", f"/track-selections/{sel_id}/lock", token=mgr_tok)
    print(f"   곡 {len(SONGS)}개 선택/잠금")

    setlist = api.call("POST", "/setlists", token=mgr_tok,
                       body={"trackSelectionId": sel_id, "title": "2023 겨울 연합공연"})
    setlist_id = api.pick_id(setlist, "setlistId")
    print(f"== 4. 셋리스트 {setlist_id} ==")

    print("== 5. 가용성 ==")
    for name, rules, excs in MEMBERS:
        weekly = [{"dayOfWeek": d, "startSlot": s, "endSlot": e}
                  for d, ranges in rules.items() for (s, e) in ranges]
        exceptions = [{"date": dt, "kind": k, **({"startSlot": s, "endSlot": e} if s is not None else {})}
                      for (dt, k, s, e) in excs]
        api.call("PUT", "/me/availability", token=token[name],
                 body={"effectiveFrom": WINDOW[0], "effectiveTo": WINDOW[1],
                       "weeklyRules": weekly, "exceptions": exceptions, "note": name})
    print(f"   {len(MEMBERS)}명 등록")

    print("== 6. 자동배치 ==")
    board = api.call("POST", f"/setlists/{setlist_id}/schedule-boards", token=mgr_tok,
                     body={"name": "2023 자동배치 검증", "boardTimeRangeFrom": 26, "boardTimeRangeTo": 46,
                           "windowFrom": WINDOW[0], "windowTo": WINDOW[1]})
    board_id = api.pick_id(board, "boardId")
    result = api.call("POST", f"/setlists/{setlist_id}/schedule-boards/{board_id}/auto-schedule",
                      token=mgr_tok, body=AUTO_PLACE_REQUEST)

    tracks = api.call("GET", f"/setlists/{setlist_id}/tracks", token=mgr_tok, params={"pageSize": 100})
    rows = tracks.get("content", tracks) if isinstance(tracks, dict) else tracks
    title_by_track = {t["setlistTrackId"]: t["title"] for t in rows}

    blocks = []
    for b in result["blocks"]:
        d = datetime.date.fromisoformat(b["startDate"])
        title = "/".join(title_by_track.get(t, t[:8]) for t in b["trackIds"])
        blocks.append((d, b["startSlot"], b["endSlot"], title))
    blocks.sort()

    print(f"\n== 생성 블록 {len(blocks)}개 ==")
    cur = None
    base = datetime.date(2023, 1, 9)
    for d, s, e, title in blocks:
        wk = (d - base).days // 7 + 1
        if wk != cur:
            cur = wk
            print(f"--- {wk}주차 ---")
        print(f"  {d} ({DAYS[d.weekday()][:3]}) {slot_hhmm(s)}-{slot_hhmm(e)}  {title}")

    placed = {}
    for d, s, _, title in blocks:
        placed.setdefault(title, []).append((DAYS[d.weekday()], s))

    print(f"\n== 사람 시간표 대비 ({len(EXPECTED)}곡 기준) ==")
    day_ok = slot_ok = 0
    for title, (exp_day, exp_slot) in sorted(EXPECTED.items()):
        got = placed.get(title, [])
        modal = Counter(got).most_common(1)[0][0] if got else None
        d_ok = modal is not None and modal[0] == exp_day
        s_ok = d_ok and modal[1] == exp_slot
        day_ok += d_ok
        slot_ok += s_ok
        got_s = f"{modal[0][:3]} {slot_hhmm(modal[1])} x{len(got)}" if modal else "미배치"
        print(f"  {'==' if s_ok else ('~=' if d_ok else '!=')} {title:<28} "
              f"기대 {exp_day[:3]} {slot_hhmm(exp_slot)} | 생성 {got_s}")

    n_songs = len(title_by_track)
    print(f"\n  배치곡 {len(placed)}/{n_songs}, 총 블록 {len(blocks)}")
    print(f"  요일 일치 {day_ok}/{len(EXPECTED)}, 요일+시각 일치 {slot_ok}/{len(EXPECTED)}")
    missing = sorted(set(title_by_track.values()) - set(placed))
    if missing:
        print(f"  미배치: {', '.join(missing)}")


if __name__ == "__main__":
    main()

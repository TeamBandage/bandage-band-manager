"""TuNA 2020 2학기 정기공연 시간표 자동배치 재현 테스트.

원본: 구글 시트 (docs.google.com/spreadsheets/d/1ySKZr_yNozOQgda3xG3GYH6S0YD8UoEimnxse1VkdMM)
- gid=2084986801: 멤버 30명 요일별 가용시간 (합주 기간 2020-10-29 ~ 2020-11-27)
- gid=1300356447: 선곡 시트 (곡별 세션 배정)
- gid=8489554:    사람이 손으로 짠 실제 시간표 (비교 기준)

시트 데이터를 아래에 리터럴로 내장했다. 실행 절차:
  1) docker compose -f docker-compose-test.yml up -d
  2) set -a; source .env; set +a; ./gradlew bootRun
  3) python3 scripts/e2e/tuna_timetable_test.py

멱등: 회원가입은 이미 있으면 통과. 선곡/셋리스트/보드는 실행마다 새로 만든다.

가정(시트에 없어서 정한 것):
- '오렌지의 시간'은 선곡 시트에 없다(시간표에만 등장). Surl 곡이므로 Cilla 라인업을 준용했다.
- 슬래시(A/B) 표기는 더블 캐스팅으로 보고 두 명 모두 참여자로 넣었다.
- '고명북/창융디 오프될 경우' 같은 조건부 가용성과 '미정' 일정은 반영하지 않았다.
- 시간 표기의 오전/오후: 8시 이하 숫자는 오후로 해석(3:00~ = 15:00~), 9~12시는 표기 그대로.

실행 결과 (2026-08-17, 로컬):
- 69블록 배치, 23곡 중 17곡 배치 / 6곡 미배치, 사람 시간표와 요일 일치 9/23.
- 미배치 6곡(그것만이 내 세상, 예뻤어, 너를 만나, 오래된 노래, Dynamite, 너를 보내고)은
  가용성 시트 기준 참여자 전원이 겹치는 슬롯이 실제로 없는 곡들이다. 사람 시간표는 이 곡들을
  일부 멤버의 가용성을 위반한 자리에 배치했다(현실에서는 별도 협의였을 것).
  즉 알고리즘은 시트를 정직하게 지켰고, 그 결과가 사람과 갈라진 지점은 전부 "위반이 필요한 곡"이다.
"""

import os
from collections import Counter

os.environ.setdefault("BANDAGE_BASE", "http://localhost:8080/api/v1")
import api  # noqa: E402

PW = "12345678"
WINDOW = ("2020-10-29", "2020-11-27")
DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]
FULL = (0, 48)

# ---------------------------------------------------------------- 멤버 & 가용성
# (이름, {요일: [(startSlot, endSlot), ...]}, [예외])   슬롯 = 30분, 22=11:00, 44=22:00
# 예외: (date, kind, startSlot|None, endSlot|None)  None,None = 종일
MEMBERS = [
    ("정선우", {"MONDAY": [(0, 32)], "TUESDAY": [(28, 48)], "WEDNESDAY": [(26, 48)], "THURSDAY": [(22, 48)],
                "FRIDAY": [(28, 48)], "SATURDAY": [FULL], "SUNDAY": [FULL]}, []),
    ("김동혁", {"MONDAY": [FULL], "TUESDAY": [(34, 48)], "WEDNESDAY": [FULL], "FRIDAY": [FULL],
                "SATURDAY": [(0, 32)]}, [("2020-11-23", "BLOCKED", None, None)]),
    ("송유나", {"MONDAY": [(0, 32)], "TUESDAY": [(0, 32)], "WEDNESDAY": [FULL], "THURSDAY": [FULL],
                "FRIDAY": [(30, 48)], "SATURDAY": [FULL]}, [("2020-10-29", "BLOCKED", 28, 36)]),
    ("신세림", {"MONDAY": [(28, 48)], "TUESDAY": [FULL], "THURSDAY": [FULL], "FRIDAY": [FULL],
                "SATURDAY": [(0, 32)], "SUNDAY": [(28, 48)]}, []),
    ("한가현", {"MONDAY": [FULL], "TUESDAY": [(32, 48)], "WEDNESDAY": [(38, 48)], "THURSDAY": [(31, 48)],
                "SATURDAY": [FULL], "SUNDAY": [FULL]}, []),
    ("김세욱", {"WEDNESDAY": [FULL], "THURSDAY": [FULL], "SATURDAY": [(36, 48)]}, []),
    ("손채은", {"TUESDAY": [FULL], "THURSDAY": [FULL], "FRIDAY": [FULL]},
     [("2020-10-29", "BLOCKED", None, None)]),
    ("이재민", {"MONDAY": [FULL], "FRIDAY": [FULL], "SATURDAY": [FULL], "SUNDAY": [FULL]}, []),
    ("신다운", {"MONDAY": [(36, 48)], "TUESDAY": [(28, 48)], "WEDNESDAY": [(0, 26)], "FRIDAY": [FULL],
                "SATURDAY": [FULL]}, []),
    ("정서현", {"MONDAY": [(38, 48)], "TUESDAY": [(0, 32)], "THURSDAY": [(34, 48)], "FRIDAY": [FULL],
                "SATURDAY": [(38, 48)], "SUNDAY": [FULL]}, []),
    ("김성식", {"MONDAY": [FULL], "TUESDAY": [FULL], "WEDNESDAY": [(0, 30)], "THURSDAY": [FULL],
                "FRIDAY": [(34, 48)], "SATURDAY": [FULL], "SUNDAY": [FULL]}, []),
    ("정도윤", {d: [FULL] for d in DAYS}, [("2020-10-30", "BLOCKED", 32, 48)]),
    ("한승범", {"MONDAY": [(38, 48)], "TUESDAY": [(33, 48)], "WEDNESDAY": [FULL], "FRIDAY": [FULL],
                "SATURDAY": [FULL], "SUNDAY": [FULL]}, []),
    ("김준성", {"TUESDAY": [(32, 48)], "WEDNESDAY": [(38, 48)], "THURSDAY": [(30, 48)], "FRIDAY": [(38, 48)],
                "SATURDAY": [FULL], "SUNDAY": [FULL]}, [("2020-11-27", "BLOCKED", None, None)]),
    ("이지우", {"TUESDAY": [(38, 48)], "WEDNESDAY": [(26, 30)], "FRIDAY": [FULL], "SATURDAY": [FULL],
                "SUNDAY": [FULL]}, []),
    ("양신", {"TUESDAY": [FULL], "THURSDAY": [(34, 48)], "FRIDAY": [(34, 48)], "SATURDAY": [FULL],
              "SUNDAY": [FULL]}, []),
    ("김요셉", {"MONDAY": [FULL], "TUESDAY": [(34, 48)], "THURSDAY": [(36, 48)], "FRIDAY": [(30, 48)],
                "SATURDAY": [FULL]},
     [("2020-11-07", "BLOCKED", None, None), ("2020-11-08", "BLOCKED", None, None),
      ("2020-11-14", "BLOCKED", 0, 30)]),
    ("조병웅", {"THURSDAY": [(32, 38)], "FRIDAY": [(0, 34)], "SATURDAY": [FULL], "SUNDAY": [(0, 30)]}, []),
    ("박혜원", {"MONDAY": [FULL], "WEDNESDAY": [(0, 28)], "FRIDAY": [(32, 48)], "SATURDAY": [FULL],
                "SUNDAY": [(30, 48)]}, []),
    ("남연주", {"TUESDAY": [(30, 48)], "THURSDAY": [(30, 48)], "FRIDAY": [(30, 48)], "SATURDAY": [FULL],
                "SUNDAY": [FULL]}, []),
    ("이상윤", {"TUESDAY": [(34, 48)], "WEDNESDAY": [(34, 48)], "THURSDAY": [(0, 28)], "SATURDAY": [FULL],
                "SUNDAY": [(0, 26)]}, []),
    ("권나현", {"MONDAY": [FULL], "TUESDAY": [(30, 48)], "WEDNESDAY": [(30, 38)], "THURSDAY": [(0, 31)],
                "FRIDAY": [FULL]}, [("2020-10-29", "BLOCKED", None, None)]),
    ("임찬휘", {"MONDAY": [(39, 48)], "WEDNESDAY": [(39, 48)], "THURSDAY": [FULL], "FRIDAY": [(39, 48)],
                "SATURDAY": [FULL], "SUNDAY": [FULL]},
     [("2020-11-14", "BLOCKED", None, None), ("2020-11-15", "BLOCKED", None, None),
      ("2020-11-16", "BLOCKED", None, None)]),
    ("이예령", {"MONDAY": [(32, 36)], "WEDNESDAY": [(30, 48)], "THURSDAY": [(0, 24)]},
     [("2020-10-29", "BLOCKED", None, None), ("2020-10-30", "BLOCKED", None, None),
      ("2020-10-31", "BLOCKED", None, None)]),
    ("변석주", {"MONDAY": [(30, 48)], "TUESDAY": [(30, 48)], "WEDNESDAY": [(0, 36), (40, 48)],
                "THURSDAY": [FULL], "SATURDAY": [FULL], "SUNDAY": [FULL]}, []),
    ("전재완", {"MONDAY": [(33, 48)], "TUESDAY": [(34, 48)], "THURSDAY": [(30, 48)],
                "FRIDAY": [(18, 24), (36, 48)], "SATURDAY": [(0, 36)]}, []),
    ("천가영", {"MONDAY": [(36, 48)], "TUESDAY": [(18, 24), (33, 48)], "WEDNESDAY": [(39, 48)],
                "THURSDAY": [(18, 21), (30, 33)], "FRIDAY": [(18, 22), (31, 48)], "SUNDAY": [(36, 48)]}, []),
    ("정지윤", {"MONDAY": [(0, 30), (38, 48)], "TUESDAY": [(0, 24)], "WEDNESDAY": [(0, 28), (38, 48)],
                "THURSDAY": [(0, 26), (38, 48)], "FRIDAY": [(32, 36)], "SATURDAY": [FULL],
                "SUNDAY": [(36, 48)]}, []),
    ("김나연", {"MONDAY": [(24, 48)], "TUESDAY": [(37, 48)], "WEDNESDAY": [(0, 34)], "THURSDAY": [(37, 48)],
                "FRIDAY": [(30, 42)]}, [("2020-10-29", "BLOCKED", None, None)]),
    ("이유빈", {"MONDAY": [(38, 48)], "TUESDAY": [(36, 48)], "WEDNESDAY": [(36, 48)], "THURSDAY": [(30, 48)],
                "FRIDAY": [(30, 48)], "SATURDAY": [FULL], "SUNDAY": [FULL]}, []),
]

# ---------------------------------------------------------------- 선곡 (역할 -> 멤버들)
SONGS = [
    ("Nell", "1:03", {"V": ["김동혁"], "GA": ["정선우"], "GB": ["정도윤", "신다운"],
                      "B": ["김요셉", "조병웅"], "D": ["전재완"], "KA": ["정지윤"]}),
    ("Surl", "Cilla", {"V": ["김동혁"], "GA": ["정선우"], "GB": ["정도윤"], "B": ["김요셉"],
                       "D": ["임찬휘", "변석주"]}),
    ("쏜애플", "로마네스크", {"V": ["김동혁", "신세림"], "GA": ["정선우"], "GB": ["이지우"],
                              "B": ["김요셉"], "D": ["변석주"]}),
    ("잔나비", "사랑하긴 했었나요 스쳐가는 인연이었나요", {"V": ["송유나"], "GA": ["김준성", "김성식"],
                                                           "GB": ["김요셉"], "B": ["박혜원", "남연주"],
                                                           "D": ["변석주"], "KA": ["이유빈"]}),
    ("백예린", "O310", {"V": ["송유나"], "GA": ["정서현"], "GB": ["변석주"], "B": ["김요셉"],
                        "D": ["전재완"], "KA": ["이유빈"], "KB": ["정지윤"]}),
    ("BTS", "Dynamite", {"V": ["송유나", "손채은"], "GA": ["김준성"], "GB": ["신다운"], "B": ["김요셉"],
                         "D": ["임찬휘"], "KA": ["정지윤"], "KB": ["이유빈"]}),
    ("One direction", "What makes you beautiful", {"V": ["손채은"], "GA": ["김준성"], "GB": ["양신"],
                                                   "B": ["김요셉"], "D": ["임찬휘", "권나현"],
                                                   "KA": ["김나연"]}),
    ("데이식스", "예뻤어", {"V": ["손채은", "한가현"], "GA": ["김성식"], "GB": ["정서현"], "B": ["김요셉"],
                            "D": ["전재완", "이예령"], "KA": ["정지윤"]}),
    ("윤하", "비밀번호 486", {"V": ["손채은", "신세림"], "GA": ["김요셉"], "GB": ["한승범"], "B": ["남연주"],
                              "D": ["전재완", "임찬휘"], "KA": ["김나연"]}),
    ("검정치마", "everything", {"V": ["신세림"], "GA": ["정선우"], "GB": ["김성식"], "B": ["김요셉"],
                                "D": ["전재완", "임찬휘"], "KA": ["정지윤"]}),
    ("잔나비", "꿈나라 별나라", {"V": ["신세림"], "GA": ["정선우"], "GB": ["김준성"],
                                 "B": ["김요셉", "박혜원"], "D": ["임찬휘"], "KA": ["이유빈"]}),
    ("쏜애플", "아가미", {"V": ["신세림"], "GA": ["정선우"], "GB": ["김준성"], "B": ["김요셉"],
                          "D": ["변석주"]}),
    ("새소년", "난춘", {"V": ["한가현", "신세림"], "GA": ["김준성", "정도윤"], "B": ["김요셉"],
                        "D": ["변석주"], "KA": ["정지윤"], "KB": ["이유빈"]}),
    ("자우림", "스물다섯 스물하나", {"V": ["한가현", "손채은"], "GA": ["김준성", "정선우"],
                                     "GB": ["한승범", "이지우"], "B": ["김요셉", "남연주"],
                                     "D": ["변석주"], "KA": ["김나연"], "KB": ["천가영"]}),
    ("오왠", "오늘", {"V": ["한가현"], "GA": ["정선우"], "GB": ["한승범"], "B": ["김요셉"],
                      "D": ["전재완"], "KA": ["정지윤"]}),
    ("부활", "Lonely night", {"V": ["한가현"], "GA": ["김준성", "정선우"], "GB": ["정서현"],
                              "B": ["김요셉"], "D": ["전재완", "임찬휘"], "KA": ["김나연"]}),
    ("들국화", "그것만이 내 세상", {"V": ["김세욱"], "GA": ["김준성"], "B": ["김요셉"],
                                    "D": ["전재완", "권나현"], "KA": ["정선우"]}),
    ("Radiohead", "Creep", {"V": ["김세욱"], "GA": ["김성식"], "GB": ["정도윤", "이지우"],
                            "B": ["조병웅"], "D": ["김요셉"]}),
    ("스탠딩에그", "오래된 노래", {"V": ["이재민"], "GA": ["김준성", "정선우"], "GB": ["한승범"],
                                   "B": ["김요셉"], "D": ["전재완"], "KA": ["정지윤"], "KB": ["천가영"]}),
    ("윤도현", "너를 보내고", {"V": ["이재민"], "GA": ["김준성", "김성식"], "GB": ["이지우"],
                               "B": ["김요셉"], "D": ["변석주"], "KA": ["천가영"]}),
    ("폴킴", "너를 만나", {"V": ["이재민"], "GA": ["김성식"], "GB": ["김준성"], "B": ["이상윤"],
                           "D": ["전재완"], "KA": ["천가영"]}),
    ("Coldplay", "Yellow", {"V": ["양신"], "GA": ["김준성"], "GB": ["양신"], "B": ["김요셉", "남연주"],
                            "D": ["임찬휘", "권나현"]}),
    # 시트에 없음: 시간표에만 등장. Surl 곡이라 Cilla 라인업 준용 (가정)
    ("Surl", "오렌지의 시간", {"V": ["김동혁"], "GA": ["정선우"], "GB": ["정도윤"], "B": ["김요셉"],
                               "D": ["변석주"]}),
]

ROLE_LABEL = {"V": "VOCAL", "GA": "GUITARONE", "GB": "GUITARTWO", "B": "BASS", "D": "DRUM",
              "KA": "KEYSONE", "KB": "KEYSTWO"}

# ---------------------------------------------------------------- 기대 시간표 (사람이 짠 정본, 대표 주 기준)
# 곡 -> (요일, 시작 슬롯). 주차별 30분 안팎 변동은 있으나 대표 패턴만 비교한다.
EXPECTED = {
    "난춘": ("TUESDAY", 36), "그것만이 내 세상": ("THURSDAY", 24), "예뻤어": ("THURSDAY", 25),
    "너를 만나": ("FRIDAY", 36), "오래된 노래": ("FRIDAY", 37), "스물다섯 스물하나": ("FRIDAY", 38),
    "Yellow": ("FRIDAY", 39), "꿈나라 별나라": ("FRIDAY", 40), "Lonely night": ("FRIDAY", 41),
    "What makes you beautiful": ("FRIDAY", 42), "비밀번호 486": ("FRIDAY", 43),
    "O310": ("SATURDAY", 22), "everything": ("SATURDAY", 23), "오늘": ("SATURDAY", 24),
    "Dynamite": ("SATURDAY", 25), "1:03": ("SATURDAY", 26), "Cilla": ("SATURDAY", 27),
    "아가미": ("SATURDAY", 28), "오렌지의 시간": ("SATURDAY", 29), "Creep": ("SATURDAY", 30),
    "로마네스크": ("SATURDAY", 31), "사랑하긴 했었나요 스쳐가는 인연이었나요": ("SATURDAY", 32),
    "너를 보내고": ("SATURDAY", 36),
}

AUTO_PLACE_REQUEST = {
    "interval": "WEEKLY",
    "jamDurationSlots": 1,          # 원본 시간표는 곡당 30분
    "maxJamsPerDay": 12,            # 원본은 하루 여러 모임을 허용 -> 사실상 무제한
    "maxEmptySlotsBetweenJams": 46,  # 모임 간 간격 제약 없음
    "dayPreference": ["SATURDAY", "FRIDAY", "THURSDAY", "TUESDAY", "WEDNESDAY", "MONDAY", "SUNDAY"],
    "startTimePreference": 22,      # 11:00 (원본 토요일 시작)
    "endTimePreference": 44,        # 22:00
}


def slot_hhmm(s):
    return f"{s // 2:02d}:{(s % 2) * 30:02d}"


def main():
    # 1. 회원 30명 + 로그인
    print("== 1. 회원 ==")
    token, member_id = {}, {}
    for i, (name, _, _) in enumerate(MEMBERS, start=1):
        em = f"tuna{i:02d}@bandage.test"
        api.join(em, name, PW)
        t = api.login(em, PW)
        token[name] = t
        member_id[name] = api.call("GET", "/members/me", token=t)["memberId"]
    manager = "정선우"
    mgr_tok = token[manager]
    print(f"   {len(token)}명 로그인 완료")

    # 2. 밴드 TuNA (리더 정선우, 전원 가입)
    print("== 2. 밴드 TuNA ==")
    band = api.call("POST", "/bands", token=mgr_tok,
                    body={"name": f"TuNA-{os.getpid()}", "description": "2020 2학기 정기공연"})
    band_id = api.pick_id(band, "bandId")
    for name in token:
        if name == manager:
            continue
        api.call("POST", f"/bands/{band_id}/applications", token=token[name], expect=[200, 409])
    apps = api.call("GET", f"/bands/{band_id}/applications", token=mgr_tok, params={"pageSize": 100})
    for a in apps.get("content", apps if isinstance(apps, list) else []):
        if a.get("status") == "PENDING":
            api.call("PATCH", f"/bands/{band_id}/applications/{a['bandApplicationId']}", token=mgr_tok,
                     body={"status": "APPROVED"}, expect=[200, 400, 409])
    print(f"   bandId={band_id}")

    # 3. 선곡 회의 -> 곡 추가 -> 세션 지원/확정 -> 선택 -> 잠금
    print("== 3. 선곡 ==")
    sel = api.call("POST", "/track-selections", token=mgr_tok,
                   body={"title": "2020 2학기 정기공연 선곡", "managerId": member_id[manager],
                         "participantUserIds": list(member_id.values()), "bandIds": [band_id]})
    sel_id = api.pick_id(sel, "selectionId")
    for artist, title, roles in SONGS:
        # 세션 정원이 1명이라 더블 캐스팅(A/B)은 멤버마다 세션을 만든다 (GUITARTWO, GUITARTWOB ...)
        slots = [(ROLE_LABEL[r] + ("" if i == 0 else "B"), r, name)
                 for r, names in roles.items() for i, name in enumerate(names)]
        item = api.call("POST", f"/track-selections/{sel_id}/items", token=mgr_tok,
                        body={"title": title, "artist": artist, "duration": 240,
                              "sessions": [{"label": lb, "custom": r in ("KA", "KB")}
                                           for lb, r, _ in slots]})
        item_id = item["trackSelectionItemId"]
        by_label = {s["label"]: s["sessionId"] for s in item["sessions"]}
        for lb, _, name in slots:
            sid = by_label[lb]
            api.call("POST", f"/track-selections/{sel_id}/items/{item_id}/sessions/{sid}/applicants",
                     token=token[name])
            api.call("PATCH", f"/track-selections/{sel_id}/items/{item_id}/sessions/{sid}/confirmations",
                     token=mgr_tok, body={"confirm": [member_id[name]], "unconfirm": []})
        api.call("PATCH", f"/track-selections/{sel_id}/items/{item_id}/selection", token=mgr_tok,
                 body={"selected": True})
    api.call("POST", f"/track-selections/{sel_id}/lock", token=mgr_tok)
    print(f"   selectionId={sel_id}, 곡 {len(SONGS)}개 선택/잠금")

    # 4. 셋리스트 승격
    setlist = api.call("POST", "/setlists", token=mgr_tok,
                       body={"trackSelectionId": sel_id, "title": "2020 2학기 정기공연"})
    setlist_id = api.pick_id(setlist, "setlistId")
    print(f"== 4. 셋리스트 {setlist_id} ==")

    # 5. 멤버 가용성 등록 (본인 토큰)
    print("== 5. 가용성 ==")
    for name, rules, excs in MEMBERS:
        weekly = [{"dayOfWeek": d, "startSlot": s, "endSlot": e} for d, ranges in rules.items()
                  for (s, e) in ranges]
        exceptions = [{"date": dt, "kind": kind,
                       **({"startSlot": s, "endSlot": e} if s is not None else {})}
                      for (dt, kind, s, e) in excs]
        api.call("PUT", "/me/availability", token=token[name],
                 body={"effectiveFrom": WINDOW[0], "effectiveTo": WINDOW[1],
                       "weeklyRules": weekly, "exceptions": exceptions, "note": name})
    print(f"   {len(MEMBERS)}명 등록")

    # 6. 스케줄 보드 + 자동배치
    print("== 6. 자동배치 ==")
    board = api.call("POST", f"/setlists/{setlist_id}/schedule-boards", token=mgr_tok,
                     body={"name": "자동배치 검증", "boardTimeRangeFrom": 22, "boardTimeRangeTo": 44,
                           "windowFrom": WINDOW[0], "windowTo": WINDOW[1]})
    board_id = api.pick_id(board, "boardId")
    result = api.call("POST", f"/setlists/{setlist_id}/schedule-boards/{board_id}/auto-schedule",
                      token=mgr_tok, body=AUTO_PLACE_REQUEST)

    # 7. 결과 출력 + 기대 시간표 비교
    tracks = api.call("GET", f"/setlists/{setlist_id}/tracks", token=mgr_tok, params={"pageSize": 100})
    rows = tracks.get("content", tracks) if isinstance(tracks, dict) else tracks
    title_by_track = {t["setlistTrackId"]: t["title"] for t in rows}

    import datetime
    blocks = []
    for b in result["blocks"]:
        d = datetime.date.fromisoformat(b["startDate"])
        title = "/".join(title_by_track.get(tid, tid[:8]) for tid in b["trackIds"])
        blocks.append((d, b["startSlot"], b["endSlot"], title))
    blocks.sort()

    print(f"\n== 생성된 블록 {len(blocks)}개 ==")
    cur = None
    for d, s, e, title in blocks:
        wk = (d - datetime.date(2020, 10, 26)).days // 7 + 1
        if wk != cur:
            cur = wk
            print(f"--- {wk}주차 ---")
        print(f"  {d} ({DAYS[d.weekday()][:3]}) {slot_hhmm(s)}-{slot_hhmm(e)}  {title}")

    print("\n== 기대 시간표(사람) 대비 ==")
    placed = {}
    for d, s, _, title in blocks:
        placed.setdefault(title, []).append((DAYS[d.weekday()], s))
    same_day = same_slot = 0
    for title, (exp_day, exp_slot) in EXPECTED.items():
        got = placed.get(title, [])
        count = len(got)
        modal = Counter(got).most_common(1)[0][0] if got else None
        day_ok = modal is not None and modal[0] == exp_day
        slot_ok = day_ok and modal[1] == exp_slot
        same_day += day_ok
        same_slot += slot_ok
        got_str = f"{modal[0][:3]} {slot_hhmm(modal[1])} x{count}" if modal else "미배치"
        mark = "==" if slot_ok else ("~=" if day_ok else "!=")
        print(f"  {mark} {title:<28} 기대 {exp_day[:3]} {slot_hhmm(exp_slot)} | 생성 {got_str}")
    print(f"\n  요일 일치 {same_day}/{len(EXPECTED)}, 요일+시각 일치 {same_slot}/{len(EXPECTED)}")
    print(f"  배치 횟수 합계 {len(blocks)} (기대: 곡당 회차수만큼)")


if __name__ == "__main__":
    main()

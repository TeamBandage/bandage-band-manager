"""전체 API 전수 검증 + 엣지케이스.

seed.py 가 만든 /tmp/bandage_seed_state.json 을 읽어 실제 리소스로 121개 엔드포인트를 호출한다.
결과는 /tmp/bandage_verify.json — 문서 생성의 원천 데이터.
"""

import json
import os
import uuid

import api

STATE = json.load(open(os.environ.get("SEED_STATE", "/tmp/bandage_seed_state.json")))
PW = STATE.get("password", "12345678")
RESULTS = []


def check(tc_id, name, expect, fn, note=""):
    """expect: 허용 상태코드 리스트. 실제 상태가 벗어나면 FAIL."""
    try:
        st, body = fn()
    except Exception as e:
        RESULTS.append({"id": tc_id, "name": name, "expect": expect, "actual": "EXC",
                        "pass": False, "detail": str(e)[:300], "note": note})
        print(f"  [{tc_id}] EXC  {name}: {str(e)[:150]}")
        return None
    ok = st in expect
    msg = ""
    if isinstance(body, dict):
        msg = body.get("code") or body.get("message") or ""
    RESULTS.append({"id": tc_id, "name": name, "expect": expect, "actual": st, "pass": ok,
                    "detail": str(msg)[:200], "note": note})
    print(f"  [{tc_id}] {'PASS' if ok else 'FAIL'} {name} -> {st} {str(msg)[:100]}")
    return body


def C(method, path, token=None, body=None, params=None):
    """항상 (status, body) 를 돌려받는다 — check() 가 상태코드를 판정한다."""
    return lambda: api.call(method, path, token=token, body=body, params=params, expect="any")


def main():
    tok = STATE["tokens"]
    bands = STATE["bands"]
    setlists = STATE["setlists"]
    perfs = STATE["performances"]
    sels = STATE["selections"]

    b1 = bands[0]
    leader = str(b1["leaderMemberId"])
    lt = tok[leader]
    member2 = str(b1["memberIds"][1])
    mt = tok[member2]
    # 다른 밴드 소속 = 권한 없는 외부인
    outsider = str(bands[1]["memberIds"][-1])
    ot = tok[outsider]
    sl = setlists[0]
    slt = tok[str(sl["managerId"])]
    pf = perfs[0]
    pt = tok[str(pf["ownerMemberId"])]
    sel = sels[0]
    BAD_UUID = "00000000-0000-0000-0000-000000000000"

    print("=== A. 인증 ===")
    check("A-01", "회원가입 정상", [200, 201], C("POST", "/members/join", body={"email": f"vf_{uuid.uuid4().hex[:8]}@bandage.test", "name": "검증계정", "password": PW}))
    check("A-02", "회원가입 중복 이메일", [400, 409], C("POST", "/members/join", body={"email": STATE["members"]["1"]["email"], "name": "중복", "password": PW}))
    check("A-03", "회원가입 이메일 형식 오류", [400], C("POST", "/members/join", body={"email": f"bad-format-{uuid.uuid4().hex[:6]}", "name": "형식오류", "password": PW}), "@Email 검증 동작 여부")
    check("A-03b", "회원가입 @만 있는 이메일", [400], C("POST", "/members/join", body={"email": f"@{uuid.uuid4().hex[:6]}.com", "name": "형식오류2", "password": PW}))
    check("A-03c", "회원가입 공백 포함 이메일", [400], C("POST", "/members/join", body={"email": f"a b{uuid.uuid4().hex[:4]}@c.com", "name": "형식오류3", "password": PW}))
    check("A-04", "회원가입 필수값 누락", [400], C("POST", "/members/join", body={"email": "a@b.com"}))
    check("A-05", "로그인 정상", [200], C("POST", "/auth/login", body={"email": STATE["members"]["1"]["email"], "password": PW}))
    check("A-06", "로그인 잘못된 비밀번호", [400, 401], C("POST", "/auth/login", body={"email": STATE["members"]["1"]["email"], "password": "wrong"}))
    check("A-07", "로그인 없는 계정", [400, 401, 404], C("POST", "/auth/login", body={"email": "nobody@bandage.test", "password": PW}))
    check("A-08", "토큰 없이 보호 API", [401], C("GET", "/members/me"))
    check("A-09", "잘못된 토큰", [401], C("GET", "/members/me", token="invalid.token.here"))
    check("A-10", "리프레시 토큰 누락", [400, 401], C("POST", "/auth/refresh", body={}))
    check("A-11", "비밀번호 변경 잘못된 현재값", [400, 401], C("PATCH", "/auth/password", token=mt, body={"currentPassword": "wrong", "newPassword": "pw12345"}))
    check("A-12", "구글 OAuth 잘못된 코드", [400, 401, 500], C("POST", "/auth/oauth/google", body={"code": "invalid"}), "외부 연동")
    check("A-13", "카카오 OAuth 잘못된 코드", [400, 401, 500], C("POST", "/auth/oauth/kakao", body={"code": "invalid"}), "외부 연동")

    print("=== B. 회원 ===")
    check("B-01", "내 정보 조회", [200], C("GET", "/members/me", token=lt))
    check("B-02", "내 정보 수정", [200], C("PATCH", "/members/me", token=mt, body={"name": f"수정된 이름 {uuid.uuid4().hex[:4]}"}))
    check("B-03", "타 회원 조회", [200], C("GET", f"/members/{b1['memberIds'][2]}", token=lt))
    check("B-04", "없는 회원 조회", [404, 400], C("GET", "/members/99999999", token=lt))
    check("B-05", "회원 검색", [200], C("GET", "/members/search", token=lt, params={"q": "멤버"}))
    check("B-06", "회원 검색 빈 결과", [200], C("GET", "/members/search", token=lt, params={"q": "존재하지않는이름ZZZ"}))
    check("B-07", "내 지표 조회", [200], C("GET", "/members/me/metrics", token=lt))
    check("B-08", "프로필 이미지 presign", [200], C("POST", "/members/me/profile-image/presigned-url", token=lt, body={"contentLength": 1024, "contentType": "image/png", "ext": "png"}))
    check("B-09", "presign 5MB 초과", [400, 413], C("POST", "/members/me/profile-image/presigned-url", token=lt, body={"contentLength": 6000000, "contentType": "image/png", "ext": "png"}))
    check("B-10", "presign 잘못된 확장자", [400], C("POST", "/members/me/profile-image/presigned-url", token=lt, body={"contentLength": 1024, "contentType": "image/png", "ext": "exe"}))

    print("=== C. 밴드 ===")
    check("C-01", "밴드 목록", [200], C("GET", "/bands", token=lt, params={"pageSize": 10}))
    check("C-02", "밴드 상세", [200], C("GET", f"/bands/{b1['bandId']}", token=lt))
    check("C-03", "없는 밴드 조회", [404], C("GET", f"/bands/{BAD_UUID}", token=lt))
    check("C-04", "잘못된 UUID 형식", [400], C("GET", "/bands/not-a-uuid", token=lt), "UUID 파싱 오류 처리")
    check("C-05", "밴드 검색", [200], C("GET", "/bands/search", token=lt, params={"pageSize": 10, "keyword": "밴드"}))
    check("C-06", "내 밴드 목록", [200], C("GET", "/bands/me", token=lt, params={"pageSize": 10}))
    check("C-07", "밴드 이름 중복 생성", [400, 409], C("POST", "/bands", token=lt, body={"name": b1["name"], "description": "중복 테스트"}))
    check("C-08", "밴드 이름 누락", [400], C("POST", "/bands", token=lt, body={"description": "이름 없음"}))
    check("C-09", "밴드 수정(리더)", [200], C("PATCH", f"/bands/{b1['bandId']}", token=lt, body={"description": f"리더가 수정한 설명 {uuid.uuid4().hex[:4]}"}))
    check("C-10", "밴드 수정(외부인)", [403, 404], C("PATCH", f"/bands/{b1['bandId']}", token=ot, body={"description": "권한 없는 수정"}))
    check("C-11", "밴드 멤버 목록", [200], C("GET", f"/bands/{b1['bandId']}/members", token=lt, params={"pageSize": 100}))
    check("C-12", "밴드 삭제(외부인)", [403, 404], C("DELETE", f"/bands/{b1['bandId']}", token=ot))
    check("C-13", "pageSize 상한 초과", [400], C("GET", "/bands", token=lt, params={"pageSize": 101}))
    check("C-14", "pageSize 0", [400], C("GET", "/bands", token=lt, params={"pageSize": 0}))
    check("C-15", "pageSize 누락(스펙상 required)", [200, 400], C("GET", "/bands", token=lt), "스펙 required 이나 실제 통과 여부")

    print("=== D. 밴드 가입 신청 ===")
    b2 = bands[1]
    newbie = str(bands[2]["memberIds"][-1])
    nt = tok[newbie]
    check("D-01", "가입 신청", [200, 201], C("POST", f"/bands/{b2['bandId']}/applications", token=nt))
    check("D-02", "중복 가입 신청", [400, 409], C("POST", f"/bands/{b2['bandId']}/applications", token=nt))
    check("D-03", "내 신청 목록", [200], C("GET", "/band-applications/me", token=nt, params={"pageSize": 10}))
    check("D-04", "특정 밴드 내 신청", [200, 404], C("GET", f"/bands/{b2['bandId']}/applications/me", token=nt))
    check("D-05", "밴드 신청 목록(리더)", [200], C("GET", f"/bands/{b2['bandId']}/applications", token=tok[str(b2['leaderMemberId'])], params={"pageSize": 50}))
    check("D-06", "밴드 신청 목록(권한없음)", [403, 404], C("GET", f"/bands/{b2['bandId']}/applications", token=ot, params={"pageSize": 50}))
    check("D-07", "신청 철회", [200, 400, 404], C("PATCH", f"/bands/{b2['bandId']}/applications/me", token=nt))
    check("D-08", "이미 멤버인 밴드에 신청", [400, 409], C("POST", f"/bands/{b1['bandId']}/applications", token=mt))

    print("=== E. 선곡(TrackSelection) ===")
    sid = sel["selectionId"]
    smt = tok[str(sel["managerId"])]
    check("E-01", "내 선곡 목록", [200], C("GET", "/track-selections/me", token=smt, params={"pageSize": 10}))
    check("E-02", "선곡 상세", [200], C("GET", f"/track-selections/{sid}", token=smt))
    check("E-03", "선곡 상세(외부인)", [403, 404], C("GET", f"/track-selections/{sid}", token=ot))
    check("E-04", "선곡 항목 목록", [200], C("GET", f"/track-selections/{sid}/items", token=smt, params={"pageSize": 50}))
    check("E-05", "없는 선곡 조회", [404], C("GET", f"/track-selections/{BAD_UUID}", token=smt))
    check("E-06", "선곡 제목 수정", [200, 400, 409], C("PATCH", f"/track-selections/{sid}", token=smt, body={"title": "수정된 선곡 제목"}), "lock 이후일 수 있음")
    api.call("POST", f"/track-selections/{sid}/lock", token=smt, expect="any")
    check("E-07", "lock 된 선곡에 항목 추가", [400, 409, 403], C("POST", f"/track-selections/{sid}/items", token=smt, body={"title": "잠긴 뒤 추가", "artist": "TOOL", "sessions": [{"label": "GUITAR", "custom": False}]}), "잠금 규칙")
    check("E-08", "중복 lock", [200, 400, 409], C("POST", f"/track-selections/{sid}/lock", token=smt))
    check("E-09", "unlock", [200, 400, 409], C("POST", f"/track-selections/{sid}/unlock", token=smt))
    check("E-10", "세션 label 숫자 포함(패턴 위반)", [400], C("POST", f"/track-selections/{sid}/items", token=smt, body={"title": "패턴테스트", "artist": "TOOL", "sessions": [{"label": "GUITAR1", "custom": False}]}))
    check("E-11", "세션 빈 배열", [400, 200, 201], C("POST", f"/track-selections/{sid}/items", token=smt, body={"title": "세션없음", "artist": "TOOL", "sessions": []}))
    check("E-12", "artist 누락", [400], C("POST", f"/track-selections/{sid}/items", token=smt, body={"title": "아티스트없음", "sessions": [{"label": "BASS", "custom": False}]}))

    print("=== F. 셋리스트 ===")
    slid = sl["setlistId"]
    check("F-01", "내 셋리스트 목록", [200], C("GET", "/setlists/me", token=slt, params={"pageSize": 10}))
    check("F-02", "셋리스트 상세", [200], C("GET", f"/setlists/{slid}", token=slt))
    check("F-03", "셋리스트 트랙 목록", [200], C("GET", f"/setlists/{slid}/tracks", token=slt, params={"pageSize": 50}))
    check("F-04", "셋리스트 참여자", [200], C("GET", f"/setlists/{slid}/participants", token=slt))
    check("F-05", "제목 부분검색", [200], C("GET", "/setlists/by-title", token=slt, params={"title": "셋리스트"}))
    check("F-06", "제목 검색 빈 결과", [200], C("GET", "/setlists/by-title", token=slt, params={"title": "절대없는제목ZZZ"}))
    check("F-07", "셋리스트 상세(외부인)", [403, 404], C("GET", f"/setlists/{slid}", token=ot))
    check("F-08", "셋리스트 수정", [200], C("PATCH", f"/setlists/{slid}", token=slt, body={"title": sl["title"]}))
    check("F-09", "없는 셋리스트", [404], C("GET", f"/setlists/{BAD_UUID}", token=slt))
    check("F-10", "trackSelectionId 없이 생성", [400], C("POST", "/setlists", token=slt, body={"title": "원본없음"}))
    check("F-11", "존재하지 않는 선곡으로 생성", [400, 404], C("POST", "/setlists", token=slt, body={"trackSelectionId": BAD_UUID, "title": "가짜"}))

    tr = api.call("GET", f"/setlists/{slid}/tracks", token=slt, params={"pageSize": 5})
    track_id = (tr.get("content") or [{}])[0].get("setlistTrackId")
    if track_id:
        check("F-12", "트랙 단건 조회", [200], C("GET", f"/setlists/{slid}/tracks/{track_id}", token=slt))
        check("F-13", "트랙 수정", [200], C("PATCH", f"/setlists/{slid}/tracks/{track_id}", token=slt, body={"note": "검증 수정"}))
        check("F-14", "트랙 수정(외부인)", [403, 404], C("PATCH", f"/setlists/{slid}/tracks/{track_id}", token=ot, body={"note": "권한없음"}))

    print("=== G. 스케줄 보드 ===")
    bid_board = sl.get("boardId")
    check("G-01", "보드 목록", [200], C("GET", f"/setlists/{slid}/schedule-boards", token=slt))
    if bid_board:
        check("G-02", "보드 수정", [200], C("PATCH", f"/setlists/{slid}/schedule-boards/{bid_board}", token=slt, body={"name": "수정된 보드"}))
        blk = str(uuid.uuid4())
        if track_id:
            check("G-03", "블록 생성(upsert)", [200, 201], C("PUT", f"/setlists/{slid}/schedule-boards/{bid_board}/blocks/{blk}", token=slt,
                                                          body={"title": "합주 1", "startDate": "2026-08-20", "endDate": "2026-08-20",
                                                                "startSlot": 36, "endSlot": 40, "trackIds": [track_id],
                                                                "recurrence": {"freq": "WEEKLY", "interval": 1, "count": 3}}))
            check("G-04", "블록 슬롯 범위 초과", [400], C("PUT", f"/setlists/{slid}/schedule-boards/{bid_board}/blocks/{str(uuid.uuid4())}", token=slt,
                                                    body={"title": "범위초과", "startDate": "2026-08-20", "endDate": "2026-08-20",
                                                          "startSlot": 0, "endSlot": 99, "trackIds": [track_id]}))
            check("G-05", "블록 trackIds 빈 배열", [400], C("PUT", f"/setlists/{slid}/schedule-boards/{bid_board}/blocks/{str(uuid.uuid4())}", token=slt,
                                                      body={"title": "트랙없음", "startDate": "2026-08-20", "endDate": "2026-08-20",
                                                            "startSlot": 10, "endSlot": 12, "trackIds": []}))
            check("G-06", "블록 핀 설정", [200], C("PATCH", f"/setlists/{slid}/schedule-boards/{bid_board}/blocks/{blk}/pin", token=slt, body={"pinned": True}))
            check("G-07", "블록 삭제", [200, 204], C("DELETE", f"/setlists/{slid}/schedule-boards/{bid_board}/blocks/{blk}", token=slt))
        check("G-08", "보드 생성(외부인)", [403, 404], C("POST", f"/setlists/{slid}/schedule-boards", token=ot, body={"name": "무단 보드"}))

    print("=== H. 가용시간 ===")
    check("H-01", "가용시간 조회", [200], C("GET", "/me/availability", token=lt))
    check("H-02", "기간 조회", [200], C("GET", "/me/availability/period", token=lt, params={"from": "2026-08-10", "to": "2026-09-10"}))
    check("H-03", "슬롯 조회", [200], C("GET", "/me/availability/slots", token=lt, params={"from": "2026-08-10", "to": "2026-08-20"}))
    check("H-04", "역전 기간(from>to)", [400, 200], C("GET", "/me/availability/period", token=lt, params={"from": "2026-09-10", "to": "2026-08-10"}), "역전 구간 검증")
    check("H-05", "잘못된 날짜 형식", [400], C("GET", "/me/availability/period", token=lt, params={"from": "2026/08/10", "to": "2026-09-10"}))
    check("H-06", "슬롯 범위 초과 등록", [400], C("PUT", "/me/availability", token=lt, body={"effectiveFrom": "2026-08-10", "effectiveTo": "2026-09-10", "weeklyRules": [{"dayOfWeek": "MONDAY", "startSlot": 0, "endSlot": 99}], "exceptions": []}))
    check("H-07", "start>end 슬롯", [400, 200], C("PUT", "/me/availability", token=lt, body={"effectiveFrom": "2026-08-10", "effectiveTo": "2026-09-10", "weeklyRules": [{"dayOfWeek": "MONDAY", "startSlot": 40, "endSlot": 20}], "exceptions": []}), "역전 슬롯 검증")
    check("H-08", "잘못된 요일", [400], C("PUT", "/me/availability", token=lt, body={"effectiveFrom": "2026-08-10", "effectiveTo": "2026-09-10", "weeklyRules": [{"dayOfWeek": "FUNDAY", "startSlot": 10, "endSlot": 20}], "exceptions": []}))

    print("=== I. 공연 ===")
    pid = pf["performanceId"]
    check("I-01", "공연 목록", [200], C("GET", "/performances", token=pt, params={"pageSize": 10}))
    check("I-02", "공연 상세", [200], C("GET", f"/performances/{pid}", token=pt))
    check("I-03", "내 공연", [200], C("GET", "/performances/me", token=pt, params={"pageSize": 10}))
    check("I-04", "공연 검색", [200], C("GET", "/performances/search", token=pt, params={"pageSize": 10, "keyword": "공연"}))
    check("I-05", "밴드별 공연", [200], C("GET", "/performances", token=pt, params={"pageSize": 10, "bandId": b1["bandId"]}))
    check("I-06", "과거 시각 공연 생성", [400], C("POST", "/performances", token=pt, body={"title": "과거 공연", "startAt": "2020-01-01 19:00", "durationMinutes": 60}))
    check("I-07", "durationMinutes 0", [400], C("POST", "/performances", token=pt, body={"title": "0분 공연", "startAt": "2026-12-01 19:00", "durationMinutes": 0}))
    check("I-08", "잘못된 날짜 형식", [400], C("POST", "/performances", token=pt, body={"title": "형식오류", "startAt": "2026-12-01T19:00:00Z", "durationMinutes": 60}))
    check("I-09", "타 밴드 셋리스트로 생성", [403, 400], C("POST", "/performances", token=pt, body={"title": "무단 셋리스트", "startAt": "2026-12-01 19:00", "durationMinutes": 60, "setlistIds": [setlists[-1]["setlistId"]]}), "SETLIST_FORBIDDEN 확인")
    check("I-10", "공연 수정(오너)", [200], C("PATCH", f"/performances/{pid}", token=pt, body={"venue": "검증 수정 장소"}))
    check("I-11", "공연 수정(외부인)", [403, 404], C("PATCH", f"/performances/{pid}", token=ot, body={"venue": "무단 수정"}))
    check("I-12", "공연 셋리스트 트랙", [200], C("GET", f"/performances/{pid}/setlists/tracks", token=pt))
    check("I-13", "없는 공연 조회", [404], C("GET", f"/performances/{BAD_UUID}", token=pt))
    check("I-14", "초대 목록", [200], C("GET", f"/performances/{pid}/invitations", token=pt))
    check("I-15", "내 초대", [200], C("GET", "/performances/invitations/me", token=pt, params={"pageSize": 10}))
    check("I-16", "없는 회원 초대", [400, 404], C("POST", f"/performances/{pid}/invitations", token=pt, body={"memberId": 99999999}))
    check("I-17", "자기 자신 초대", [400, 409], C("POST", f"/performances/{pid}/invitations", token=pt, body={"memberId": pf["ownerMemberId"]}))
    check("I-18", "공연 삭제(외부인)", [403, 404], C("DELETE", f"/performances/{pid}", token=ot))

    print("=== J. 포스터 ===")
    poster = (STATE["posters"] or [{}])[0]
    check("J-01", "포스터 목록", [200], C("GET", "/performance-posters", token=pt, params={"pageSize": 10}))
    check("J-02", "내 포스터", [200], C("GET", "/performance-posters/me", token=pt, params={"pageSize": 10}))
    check("J-03", "공연별 포스터", [200], C("GET", "/performance-posters", token=pt, params={"pageSize": 10, "performanceId": pid}))
    if poster.get("posterId"):
        check("J-04", "포스터 상세", [200], C("GET", f"/performance-posters/{poster['posterId']}", token=pt))
        check("J-05", "포스터 수정", [200], C("PATCH", f"/performance-posters/{poster['posterId']}", token=pt, body={"description": "검증 수정 설명"}))
        check("J-06", "포스터 수정(외부인)", [403, 404], C("PATCH", f"/performance-posters/{poster['posterId']}", token=ot, body={"description": "무단"}))
    check("J-07", "presign 권한 없음", [403, 404], C("POST", "/performance-posters/presigned-url", token=ot, params={"performanceId": pid}, body={"contentLength": 1024, "contentType": "image/png", "ext": "png"}))
    check("J-08", "없는 imageKey 등록", [400, 404], C("POST", "/performance-posters", token=pt, body={"performanceId": pid, "imageKey": "poster/does/not/exist.png"}), "S3 존재 검증 여부")

    print("=== K. 합주(Jam) ===")
    check("K-01", "합주 목록", [200], C("GET", "/jams", token=lt, params={"pageSize": 10}))
    check("K-02", "내 합주", [200], C("GET", "/jams/me", token=lt, params={"pageSize": 10}))
    check("K-03", "내 합주 검색", [200], C("GET", "/jams/me/search", token=lt, params={"pageSize": 10, "keyword": "합주"}))
    check("K-04", "밴드별 합주", [200], C("GET", "/jams", token=lt, params={"pageSize": 10, "bandId": b1["bandId"]}))
    check("K-05", "없는 합주 조회", [404], C("GET", f"/jams/{BAD_UUID}", token=lt))

    print("=== L. 알림 ===")
    check("L-01", "알림 목록", [200], C("GET", "/notifications", token=lt, params={"pageSize": 10}))
    check("L-02", "미읽음 수", [200], C("GET", "/notifications/unread-count", token=lt))
    check("L-03", "전체 읽음", [200], C("GET" if False else "PATCH", "/notifications/read-all", token=lt))
    check("L-04", "없는 알림 읽음(UUID)", [404], C("PATCH", f"/notifications/{BAD_UUID}/read", token=lt))
    check("L-05", "알림 ID 타입 불일치(숫자)", [400], C("PATCH", "/notifications/99999999/read", token=lt), "타입 파싱 실패 처리")
    _nt = api.call("GET", "/notifications", token=lt, params={"pageSize": 3}).get("content") or []
    if _nt:
        check("L-06", "타인 알림 읽음", [403], C("PATCH", f"/notifications/{_nt[0]['id']}/read", token=ot))
        check("L-07", "본인 알림 읽음", [200], C("PATCH", f"/notifications/{_nt[0]['id']}/read", token=lt))

    print("=== M. 권한/위임 ===")
    b3 = bands[2]
    b3lt = tok[str(b3["leaderMemberId"])]
    _bm = api.call("GET", f"/bands/{b3['bandId']}/members", token=b3lt, params={"pageSize": 5}).get("content") or []
    _bmid = (_bm[1].get("bandMemberId") if len(_bm) > 1 else None)
    if _bmid:
        check("M-01", "역할 변경(외부인)", [403], C("PATCH", f"/bands/{b3['bandId']}/members/{_bmid}/role", token=ot, body={"role": "ADMIN"}))
        _cur = next((x.get("role") for x in _bm if x.get("bandMemberId") == _bmid), None)
        _next = "MEMBER" if _cur == "ADMIN" else "ADMIN"
        check("M-01b", f"역할 변경(리더, {_cur}->{_next})", [200], C("PATCH", f"/bands/{b3['bandId']}/members/{_bmid}/role", token=b3lt, body={"role": _next}))
        check("M-01c", "잘못된 role 값", [400], C("PATCH", f"/bands/{b3['bandId']}/members/{_bmid}/role", token=b3lt, body={"role": "KING"}))
        check("M-01d", "bandMemberId 타입 불일치(숫자)", [400], C("PATCH", f"/bands/{b3['bandId']}/members/{b3['memberIds'][1]}/role", token=b3lt, body={"role": "ADMIN"}), "타입 파싱 실패 처리")
    check("M-02", "셋리스트 매니저 위임(비매니저)", [403, 404], C("PATCH", f"/setlists/{slid}/manager", token=ot, body={"managerId": b1["memberIds"][2]}))
    check("M-03", "선곡 매니저 위임(비매니저)", [403, 404], C("PATCH", f"/track-selections/{sid}/manager", token=ot, body={"managerId": b1["memberIds"][2]}))
    check("M-04", "공연 오너 위임(비오너)", [403, 404], C("PATCH", f"/performances/{pid}/owner", token=ot, body={"targetMemberId": b1["memberIds"][2]}))

    print("=== N. 페이징/커서 ===")
    check("N-01", "커서 페이징 1p", [200], C("GET", "/bands", token=lt, params={"pageSize": 3}))
    first = api.call("GET", "/bands", token=lt, params={"pageSize": 3})
    if first.get("nextCursor"):
        check("N-02", "커서 페이징 2p", [200], C("GET", "/bands", token=lt, params={"pageSize": 3, "lastId": first["nextCursor"]}))
    check("N-03", "잘못된 커서", [400, 200], C("GET", "/bands", token=lt, params={"pageSize": 3, "lastId": "not-a-uuid"}))

    out = {"results": RESULTS,
           "summary": {"total": len(RESULTS), "pass": sum(1 for x in RESULTS if x["pass"]),
                       "fail": sum(1 for x in RESULTS if not x["pass"])},
           "apiCalls": api.summary()["total"]}
    json.dump(out, open("/tmp/bandage_verify.json", "w"), ensure_ascii=False, indent=1)
    print(f"\n=== 검증 {out['summary']['pass']}/{out['summary']['total']} PASS, {out['summary']['fail']} FAIL ===")
    for x in RESULTS:
        if not x["pass"]:
            print(f"  FAIL [{x['id']}] {x['name']}: expect {x['expect']} got {x['actual']} {x['detail'][:120]}")


if __name__ == "__main__":
    main()

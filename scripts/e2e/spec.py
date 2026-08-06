"""테스트 데이터 명세. 결정론적 — 시드를 다시 돌리면 같은 데이터가 나온다."""

import random

SEED = 20260806
BANDS = 6
MEMBERS_PER_BAND = 20
SETLISTS_PER_BAND = 10
TRACKS_PER_SETLIST = 15
PERFORMANCES_PER_BAND = 3
PASSWORD = "12345678"  # 프론트 규약: 8자 이상

# 요구된 5팀. (제목, 아티스트, 앨범, 길이초)
CATALOG = [
    ("Pull Me Under", "Dream Theater", "Images and Words", 494),
    ("Metropolis Pt. 1", "Dream Theater", "Images and Words", 564),
    ("Take the Time", "Dream Theater", "Images and Words", 501),
    ("Under a Glass Moon", "Dream Theater", "Images and Words", 425),
    ("Panic Attack", "Dream Theater", "Octavarium", 493),
    ("The Dance of Eternity", "Dream Theater", "Scenes from a Memory", 386),
    ("As I Am", "Dream Theater", "Train of Thought", 447),
    ("Schism", "TOOL", "Lateralus", 407),
    ("Lateralus", "TOOL", "Lateralus", 562),
    ("Sober", "TOOL", "Undertow", 306),
    ("Stinkfist", "TOOL", "Aenima", 311),
    ("Forty Six & 2", "TOOL", "Aenima", 363),
    ("Vicarious", "TOOL", "10000 Days", 434),
    ("The Pot", "TOOL", "10000 Days", 381),
    ("문제없어요", "쏜애플", "Blooming", 271),
    ("계절학기", "쏜애플", "계절학기", 292),
    ("시퍼런 봄", "쏜애플", "이상 기후", 262),
    ("초행", "쏜애플", "이상 기후", 305),
    ("난 있으나 마나", "쏜애플", "Blooming", 248),
    ("빛과 실", "쏜애플", "이상 기후", 288),
    ("Master of Puppets", "Metallica", "Master of Puppets", 515),
    ("Enter Sandman", "Metallica", "Metallica", 331),
    ("One", "Metallica", "...And Justice for All", 446),
    ("Fade to Black", "Metallica", "Ride the Lightning", 417),
    ("Nothing Else Matters", "Metallica", "Metallica", 388),
    ("Battery", "Metallica", "Master of Puppets", 312),
    ("Sad But True", "Metallica", "Metallica", 324),
    ("Pretender", "Official HIGE DANdism", "Traveler", 331),
    ("Mixed Nuts", "Official HIGE DANdism", "Mixed Nuts", 199),
    ("Subtitle", "Official HIGE DANdism", "Editorial", 337),
    ("宿命", "Official HIGE DANdism", "Traveler", 274),
    ("I LOVE...", "Official HIGE DANdism", "Traveler", 254),
    ("Cry Baby", "Official HIGE DANdism", "Editorial", 219),
    ("ノーダウト", "Official HIGE DANdism", "Escapade", 253),
]

BASE_SESSIONS = ["VOCAL", "GUITAR", "BASS", "DRUM"]
EXTRA_SESSIONS = ["KEYBOARD", "SYNTH", "VIOLIN", "SAXOPHONE"]

BAND_NAMES = [f"밴드 {i}" for i in range(1, BANDS + 1)]
VENUES = ["홍대 롤링홀", "클럽 FF", "상수 벨로주", "합정 무브홀", "신촌 스팀펑크", "이태원 케이크샵"]


def rng():
    return random.Random(SEED)


def member_login_id(idx):
    """member1 ... memberN — 요구사항대로 변수명을 그대로 아이디로 쓴다."""
    return f"member{idx}"


def member_email(idx):
    return f"member{idx}@bandage.test"


def member_name(idx):
    return f"멤버 {idx}"


def build_plan():
    """밴드별 멤버 배정 계획. 전역 멤버 인덱스는 1부터 연속."""
    r = rng()
    plan = {"bands": [], "total_members": 0}
    next_idx = 1
    for b in range(1, BANDS + 1):
        members = list(range(next_idx, next_idx + MEMBERS_PER_BAND))
        next_idx += MEMBERS_PER_BAND
        plan["bands"].append(
            {
                "no": b,
                "name": BAND_NAMES[b - 1],
                "description": f"{BAND_NAMES[b - 1]}의 소개입니다. E2E 테스트용 밴드.",
                "leader": members[0],
                "members": members,
                "cross_joiners": [],  # 아래에서 채움
            }
        )
    plan["total_members"] = next_idx - 1

    # 각 밴드 10%(2명)는 다른 밴드 1개 이상에 추가 가입
    for b in plan["bands"]:
        pool = [x for x in b["members"] if x != b["leader"]]
        for m in r.sample(pool, 2):
            others = [o["no"] for o in plan["bands"] if o["no"] != b["no"]]
            b["cross_joiners"].append({"member": m, "target_band": r.choice(others)})
    return plan


def tracks_for_setlist(band_no, setlist_no, r):
    """15곡. 셋리스트당 약 10%(2곡)는 같은 밴드의 다른 셋리스트와 의도적으로 중복."""
    shared = CATALOG[(band_no * 3) % len(CATALOG) : (band_no * 3) % len(CATALOG) + 2]
    if len(shared) < 2:
        shared = CATALOG[:2]
    rest_pool = [t for t in CATALOG if t not in shared]
    picked = r.sample(rest_pool, TRACKS_PER_SETLIST - len(shared))
    out = list(shared) + picked
    r.shuffle(out)
    return out


def sessions_for(title, r):
    """80% 일반(4세션), 20% 특이(세션 추가/축소)."""
    if r.random() < 0.8:
        return [{"label": s, "custom": False} for s in BASE_SESSIONS]
    if r.random() < 0.5:
        return [{"label": s, "custom": False} for s in BASE_SESSIONS] + [
            {"label": r.choice(EXTRA_SESSIONS), "custom": True}
        ]
    return [{"label": s, "custom": False} for s in r.sample(BASE_SESSIONS, 2)]

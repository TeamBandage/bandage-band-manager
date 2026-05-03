# Schedule API E2E 검증 리포트 (skeleton)

## 메타

- 작성일: 2026-05-03
- 작성자: Claude (BD-16)
- 대상 브랜치: `feat/BD-16-practice-schedule-coordination`
- 대상 base URL: `http://localhost:8080` (local 프로필, ddl-auto: create)
- 검증 범위: BD-16 Schedule 도메인 13개 엔드포인트 (§8-1 ~ §8-13)
- 검증 도구: `curl`
- 상태: **대기 중 — 백엔드 미기동.** 본 문서는 시나리오 골격만 채워둔 상태이며,
  실제 요청/응답 본문은 백엔드 기동 + 테스트 데이터 시드 후 채워 넣을 예정.

## 사전 준비

1. PostgreSQL local 기동 (DB_HOST/PORT/NAME/USERNAME/PASSWORD env 설정)
2. `./gradlew bootRun --args='--spring.profiles.active=local'` (로그인 세션 / JWT 발급용)
3. 시드 데이터:
   - 사용자 3명 (manager/participantA/participantB), 각 이메일 회원가입 → 로그인하여 access token 확보
   - 밴드 1개 + 멤버 3명
   - SetlistMeeting 1개 (purpose=PERFORMANCE, performanceId 필요 시 별도 Performance 사전 생성)
   - SetlistMeetingMember 3명 (manager/participantA/participantB) 등록
   - SetlistItem 2~3건 등록 + 회의 lock (PracticeSong 매핑)
   - 환경변수: `MGR_TOKEN`, `PARTA_TOKEN`, `PARTB_TOKEN`, `MEETING_ID`, `BOARD_ID_1`, `ITEM_ID_1`, `ITEM_ID_2`

## 시나리오

### §8-1 GET /schedules/me

| 케이스 | 호출자 | 기대 status | 기대 응답 / 비고 |
|---|---|---|---|
| 정상 (미입력) | participantA | 200 | `availableDates=[]`, `unavailableDates=[]`, `blocks={}`, `note=null`, `completed=false`, `updatedAt=null` |
| 정상 (입력 후) | participantA | 200 | 직전 PUT 결과 반영 |
| 비참여자 | 외부 사용자 | 403 | `SETLIST_MEETING_FORBIDDEN` |
| 회의 미존재 | participantA | 404 | `SETLIST_MEETING_NOT_FOUND` |

요청 예시:
```bash
curl -H "Authorization: Bearer $PARTA_TOKEN" \
  "http://localhost:8080/api/v1/setlist-meetings/$MEETING_ID/schedules/me"
```

실제 응답: _대기 중_

### §8-2 PUT /schedules/me

| 케이스 | body 요지 | 기대 status | 기대 코드 |
|---|---|---|---|
| 정상 | available/unavailable 분리, 정상 범위 | 200 | — |
| 날짜 중복 | available, unavailable 에 동일 일자 포함 | 400 | `SCHEDULE_DATES_OVERLAP` |
| 범위 초과 | practiceWindow 밖 일자 포함 | 400 | `SCHEDULE_DATE_OUT_OF_WINDOW` |
| note 길이 초과 | 501자 note | 400 | `INVALID_INPUT_VALUE` (Bean Validation) |
| 본인 외 사용자 | participantB 토큰으로 participantA 자원 — N/A | — | URL 자체가 `/me` 라 본인만 호출 가능 (토큰 = 본인) |

### §8-3 GET /schedules

| 케이스 | 기대 status | 비고 |
|---|---|---|
| 참여자 호출 | 200 | 응답 등록자 목록 |
| 비참여자 | 403 | `SETLIST_MEETING_FORBIDDEN` |

### §8-4 GET /schedules/aggregate

| 케이스 | 기대 status | 비고 |
|---|---|---|
| 참여자 호출 | 200 | `dateAvailability` 가 practiceWindow 일자 전체 포함, `pending = total − available − unavailable >= 0` |

### §8-5 GET /schedule-boards

| 케이스 | 기대 status | 비고 |
|---|---|---|
| 참여자 호출 | 200 | board[] (각 board.blocks[] 포함) |
| 비참여자 | 403 | — |

### §8-6 POST /schedule-boards

| 케이스 | 기대 status | 비고 |
|---|---|---|
| 매니저, 첫 시안 | 201/200 | confirmed=false, version=0 |
| 5번째 추가 후 6번째 시도 | 400 | `SCHEDULE_BOARD_LIMIT_EXCEEDED` |
| 비매니저 | 403 | `SETLIST_MEETING_NOT_MANAGER` |
| name 누락 / 51자 | 400 | Bean Validation |

### §8-7 PATCH /schedule-boards/{boardId}

| 케이스 | 기대 status | 비고 |
|---|---|---|
| 정상 부분 수정 | 200 | name 만 / paletteSeed 만 / constraints 만 |
| confirmed=true 시안 수정 | 409 | `SCHEDULE_BOARD_ALREADY_CONFIRMED` |
| 동시 수정 (version 충돌) | 409 | `SCHEDULE_BOARD_VERSION_CONFLICT` (두 세션이 같은 version 으로 PATCH) |
| boardId ↔ meetingId 불일치 | 404 | `SCHEDULE_BOARD_NOT_FOUND` |

### §8-8 DELETE /schedule-boards/{boardId}

| 케이스 | 기대 status | 비고 |
|---|---|---|
| 매니저, 미확정 시안 | 200 | 소속 block 도 함께 삭제됨을 후속 GET 으로 검증 |
| confirmed | 409 | `SCHEDULE_BOARD_ALREADY_CONFIRMED` |

### §8-9 PUT /blocks/{blockId}

| 케이스 | 기대 status | 비고 |
|---|---|---|
| 신규 (client UUIDv7) | 200 | 새 블록 생성 |
| 동일 blockId 재호출 | 200 | 같은 row 갱신 (idempotent) |
| `startSlot + durationSlots > 48` | 400 | `SCHEDULE_SLOT_INVALID` |
| date ∉ practiceWindow | 400 | `SCHEDULE_DATE_OUT_OF_WINDOW` |
| confirmed board | 409 | `SCHEDULE_BOARD_ALREADY_CONFIRMED` |
| boardId 불일치 | 404 | `SCHEDULE_BLOCK_NOT_FOUND` |

### §8-10 DELETE /blocks/{blockId}

| 케이스 | 기대 status | 비고 |
|---|---|---|
| 정상 | 200 | row 제거 |
| confirmed board | 409 | — |
| 미존재 | 404 | — |

### §8-11 PATCH /blocks/{blockId}/pin

| 케이스 | 기대 status | 비고 |
|---|---|---|
| pinned=true | 200 | block.pinned=true |
| pinned=false | 200 | block.pinned=false |
| confirmed board | 409 | — |

### §8-12 POST /confirm

| 케이스 | 기대 status | 비고 |
|---|---|---|
| 정상 (lock 상태) | 200 | `practicesCreated.size == blocks.size`, `purpose=PERFORMANCE` 면 linked.size 동일 |
| 미lock | 400 | `SETLIST_NOT_LOCKED` |
| 다른 시안이 confirmed | 409 | `SCHEDULE_BOARD_ALREADY_CONFIRMED` |
| 비매니저 | 403 | — |

### §8-13 POST /unconfirm

| 케이스 | 기대 status | 비고 |
|---|---|---|
| 정상 | 200 | board.confirmed=false, 생성된 Practice 는 GET /practices 에서 그대로 노출 |
| 미확정 시안 | 400 | `SCHEDULE_BOARD_NOT_CONFIRMED` |

## 권한 매트릭스 요약

| 경로 | 비인증 | 비참여자 | 참여자 | 매니저 |
|---|---|---|---|---|
| GET /schedules/me, /schedules, /schedules/aggregate | 401 | 403 | 200 | 200 |
| PUT /schedules/me | 401 | 403 | 200 (본인) | 200 (본인) |
| GET /schedule-boards | 401 | 403 | 200 | 200 |
| POST/PATCH/DELETE /schedule-boards | 401 | 403 | 403 | 200 |
| PUT/DELETE/PATCH /blocks | 401 | 403 | 403 | 200 |
| POST /confirm, /unconfirm | 401 | 403 | 403 | 200 |

## 권장 조치 (잠정)

- 검증 미수행: 백엔드 기동 후 본 문서를 갱신하여 P0/P1/P2 항목 분류 예정.
- 잠재 우려:
  - confirm 시 `practiceSongId` 누락된 setlist item 이 있으면 `PRACTICE_SONG_NOT_FOUND` 로 전체 롤백 — lock 시 확실히 매핑되었는지 cross-check 필요
  - 동시성: confirm 동시 호출에 대한 application-level guard 만 존재. 스트레스 시나리오 미검증

## 재현용 페이로드 위치

미작성. 검증 시 본 디렉토리에 `.json` payload fixture 별첨 예정.

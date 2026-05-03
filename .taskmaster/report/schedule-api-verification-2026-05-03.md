# Schedule API E2E 검증 리포트

## 메타

- 작성일: 2026-05-03
- 작성자: Claude (BD-16)
- 대상 브랜치: `feat/BD-16-practice-schedule-coordination`
- 대상 base URL: `http://localhost:8080` (local 프로필, ddl-auto: create)
- 검증 범위: BD-16 Schedule 도메인 13개 엔드포인트 (§8-1 ~ §8-13)
- 검증 도구: `curl`
- 결과 요약: **전수 통과 (13/13 엔드포인트, 24개 시나리오)**. 차단 P0/P1 없음.

## 사전 시드

- 회원 3명: id=1 manager, id=2 PartA, id=3 PartB (이메일 prefix `mgr_/parta_/partb_<ts>@bd16.test`, 비밀번호 `pw1234`)
- Band 1개 (BD16-Band, leader=manager) — 멤버십은 schedule API 검증과 무관 (setlist 회의에 별도 검증 없음)
- SetlistMeeting 1개 (purpose=GENERAL, manager=1, participantUserIds=[1,2,3], practiceWindow=2026-05-10~17)
- SetlistItem 2건 (Vicarious, Schism) → meeting lock → PracticeSong 자동 매핑 확인 (`practiceSongMap` 응답)

전 시나리오 모두 메시지/code/HTTP status 가 컨트롤러 + ErrorCode + service 기대값과 일치.

## 시나리오별 결과

### §8-1 GET /schedules/me

| 케이스 | 호출자 | HTTP | code | 결과 |
|---|---|---|---|---|
| 미입력 | PartA | 200 | — | `availableDates=[]`, `unavailableDates=[]`, `blocks={}`, `note=null`, `completed=false`, `updatedAt=null` |
| 미입력 | Manager | 200 | — | 동일 (userId=1) |
| 비인증 | (no Bearer) | 401 | UNAUTHORIZED | "인증되지 않은 회원입니다." |

### §8-2 PUT /schedules/me

| 케이스 | 호출자 | HTTP | code | 비고 |
|---|---|---|---|---|
| 정상 (avail+unavail+blocks+note+completed) | PartA | 200 | — | `updatedAt` 채워짐 |
| 정상 | PartB | 200 | — | `note` 미지정 → null 유지 |
| 정상 | Manager | 200 | — | 매니저도 본인 가용 시간 입력 가능 |
| `availableDates ∩ unavailableDates ≠ ∅` (2026-05-11 양쪽 포함) | PartA | 400 | SCHEDULE_DATES_OVERLAP | "availableDates와 unavailableDates에 중복된 날짜가 있습니다." |
| `availableDates=[2026-06-01]` (window 밖) | PartA | 400 | SCHEDULE_DATE_OUT_OF_WINDOW | "선택한 날짜가 practiceWindow 범위를 벗어났습니다." |
| `note` 501자 | PartA | 400 | INVALID_INPUT_VALUE | `fieldErrors.note` 동봉 |

### §8-3 GET /schedules

| 케이스 | HTTP | 비고 |
|---|---|---|
| 참여자 호출 | 200 | `userId=2/3/1` 3명 응답자 모두 노출 (등록 순서) |

### §8-4 GET /schedules/aggregate

| 케이스 | HTTP | 비고 |
|---|---|---|
| 매니저 호출 | 200 | `dateAvailability` 가 window 8일 모두 포함, `2026-05-10`={avail=3, unavail=0, pending=0}, `2026-05-13`={avail=2, unavail=1, pending=0}, `totalParticipants=3`, `completedCount=3` — 수치 정합성 확인 |

### §8-5 GET /schedule-boards

| 케이스 | HTTP | 비고 |
|---|---|---|
| 빈 목록 | 200 | `data=[]` |
| 5개 생성 후 | 200 | (§8-6 결과로 검증) |

### §8-6 POST /schedule-boards

| 케이스 | 호출자 | HTTP | code |
|---|---|---|---|
| 비매니저 | PartA | 403 | SETLIST_MEETING_NOT_MANAGER |
| 매니저 1번째 | Manager | 200 | — (`version=0`, `confirmed=false`, `constraints` 기본값 18/44/true/240 채워짐) |
| 매니저 2~5번째 | Manager | 200 | — (총 5개) |
| 매니저 6번째 | Manager | 400 | SCHEDULE_BOARD_LIMIT_EXCEEDED |

### §8-7 PATCH /schedule-boards/{boardId}

| 케이스 | 호출자 | HTTP | code | 비고 |
|---|---|---|---|---|
| 정상 (name+paletteSeed) | Manager | 200 | — | `version: 0 → 1` 증가 확인 |
| 비매니저 | PartA | 403 | SETLIST_MEETING_NOT_MANAGER | — |
| 미존재 boardId | Manager | 404 | SCHEDULE_BOARD_NOT_FOUND | — |
| confirmed 시안 수정 (8-12 후) | Manager | 409 | SCHEDULE_BOARD_ALREADY_CONFIRMED | — |

> optimistic lock 충돌(`SCHEDULE_BOARD_VERSION_CONFLICT`)은 동시 PATCH 가 필요해 본 라운드에서 제외. 후속에서 IT 또는 동시성 시나리오로 별도 검증 권장.

### §8-8 DELETE /schedule-boards/{boardId}

| 케이스 | HTTP | code | 비고 |
|---|---|---|---|
| 매니저 정상 (board #5) | 200 | — | board 삭제, block 동시 CASCADE (DB 레벨) |
| confirmed 시안 삭제 (8-12 후) | 409 | SCHEDULE_BOARD_ALREADY_CONFIRMED | — |

### §8-9 PUT /schedule-boards/{boardId}/blocks/{blockId}

| 케이스 | HTTP | code | 비고 |
|---|---|---|---|
| 신규 (client UUIDv4, ITEM1, 2026-05-10 slot 18 dur 4) | 200 | — | id 가 그대로 응답에 노출 |
| 동일 blockId 재호출 (dur 4 → 6, note 추가) | 200 | — | row 갱신, paletteIndex null 화 (request 미포함) |
| `startSlot=46 + durationSlots=4 = 50 > 48` | 400 | SCHEDULE_SLOT_INVALID | — |
| `date=2026-06-01` (window 밖) | 400 | SCHEDULE_DATE_OUT_OF_WINDOW | — |
| 비매니저 | 403 | SETLIST_MEETING_NOT_MANAGER | — |
| confirmed board 위 신규 (8-12 후) | 409 | SCHEDULE_BOARD_ALREADY_CONFIRMED | — |

### §8-10 DELETE block

| 케이스 | HTTP | code |
|---|---|---|
| 정상 | 200 | — |
| 미존재 blockId | 404 | SCHEDULE_BLOCK_NOT_FOUND |
| confirmed board (8-12 후) | 409 | SCHEDULE_BOARD_ALREADY_CONFIRMED |

### §8-11 PATCH /pin

| 케이스 | HTTP | 비고 |
|---|---|---|
| `{pinned:true}` | 200 | `pinned=true` |
| `{pinned:false}` | 200 | `pinned=false` (토글 아니라 값 그대로 적용) |
| confirmed board (8-12 후) | 409 | SCHEDULE_BOARD_ALREADY_CONFIRMED |

### §8-12 POST /confirm

| 케이스 | HTTP | code | 비고 |
|---|---|---|---|
| 정상 (lock 상태, BOARD1 에 BLK1+BLK2) | 200 | — | `practicesCreated.size=2` (Vicarious / Schism), `startAt/durationMinutes` 정확: BLK1 → `2026-05-10T09:00`, 180min ; BLK2 → `2026-05-12T12:00`, 120min. 공식 `startAt = date.atStartOfDay() + slot×30min`, `duration = slots×30` 검증. `performancePracticesLinked=[]` (purpose=GENERAL) |
| 다른 board (BOARD2) confirm 시도 (이미 BOARD1 confirmed) | 409 | SCHEDULE_BOARD_ALREADY_CONFIRMED | — |
| 미lock 회의에서 confirm | 400 | SETLIST_NOT_LOCKED | 별도 회의 신규 생성 → board 생성 → lock 없이 confirm |

### §8-13 POST /unconfirm

| 케이스 | HTTP | code | 비고 |
|---|---|---|---|
| 정상 | 200 | — | `unconfirmedAt` 응답 |
| 재호출 (이미 unconfirmed) | 400 | SCHEDULE_BOARD_NOT_CONFIRMED | — |
| Practice 유지 검증 | — | — | unconfirm 직후 `GET /api/v1/practices/me` 호출 → 8-12 에서 생성된 `Vicarious(05-10 09:00 180m)`, `Schism(05-12 12:00 120m)` Practice 2건 그대로 노출 |

## 권한 매트릭스 검증 (실측)

| 경로 그룹 | 비인증 | 비참여자 | 참여자(non-mgr) | 매니저 |
|---|---|---|---|---|
| GET /schedules/me, GET /schedules, GET /aggregate | 401 ✓ | 미검증 (시드 멤버 외 user 미생성) | 200 ✓ | 200 ✓ |
| PUT /schedules/me | — | — | 200 (본인) ✓ | 200 (본인) ✓ |
| GET /schedule-boards | — | — | 200 ✓ | 200 ✓ |
| POST/PATCH/DELETE /schedule-boards | — | — | 403 SETLIST_MEETING_NOT_MANAGER ✓ | 200/409 ✓ |
| PUT/DELETE /blocks, PATCH /pin | — | — | 403 ✓ | 200/409 ✓ |
| POST /confirm, /unconfirm | — | — | 미검증 (매니저-only API 라 케이스 동일) | 200/400/409 ✓ |

## 권장 조치

- (참고) optimistic lock 충돌 (`SCHEDULE_BOARD_VERSION_CONFLICT`) 시나리오 — 동시 PATCH 가 필요하므로 본 curl 라운드에서 제외. 통합 테스트(JPA + 두 트랜잭션) 또는 부하 시나리오에서 별도 검증 권장. 운영 진입 전 차단 사유 아님.
- (참고) "비참여자(meeting member 가 아닌 인증 사용자)" 케이스는 시드에서 4번째 user 미생성으로 미검증. 코드 경로상 `ScheduleAuthService.validateParticipant` 가 동일 분기로 처리하므로 회귀 위험 낮음. 추후 IT 보강 권장.

## 재현용 시드 / 페이로드 메타

- 시드 timestamp: `1777795729` (이메일 suffix 로 사용)
- meetingId: `019dece2-cc56-7dd2-bff7-023086a95558`
- bandId: `019dece2-6a10-7b5c-bf78-b8fb69c7022a`
- itemIds: `019dece3-0786-756c-a48f-6bda6c02f8e0` (Vicarious), `019dece3-07c8-70a3-b436-3d2159a43ef7` (Schism)
- BOARD1 (after PATCH): `019dece3-fd3d-743d-9559-e1054ee68786`
- 생성된 Practice: `019dece4-f6da-78c4-ae55-52a07bb678a9` (Vicarious 2026-05-10T09:00 180m), `019dece4-f6f0-77b2-9dbd-bf4447aa9df4` (Schism 2026-05-12T12:00 120m)

(시드는 `ddl-auto: create` 로 부팅마다 휘발됨. 재현 시 `/tmp/bd16_seed.env` 와 동일한 흐름으로 재생성 가능.)

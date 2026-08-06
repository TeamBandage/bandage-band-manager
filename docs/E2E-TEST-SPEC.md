# Bandage E2E 테스트 시나리오 & 데이터 명세

| 항목 | 값 |
|---|---|
| 대상 서버 | `https://bandage.team` (API: `/api/v1`) |
| 실행일 | 2026-08-06 |
| 실행 방식 | API 직접 호출 (Python stdlib, `scripts/e2e/`) |
| 시드 API 호출 | 10169건 (실패 9건) |
| 검증 케이스 | 138건 (PASS 129 / FAIL 9) |
| 소요 시간 | 시드 457초 |

## 1. 생성 데이터 총괄

| 리소스 | 목표 | 실제 | 달성 |
|---|---|---|---|
| 회원 | 120명 (밴드 6 × 20) | 120명 | O |
| 밴드 | 6개 | 6개 | O |
| 선곡(TrackSelection) | 60개 | 60개 | O |
| 셋리스트 | 60개 (밴드당 10) | 60개 | O |
| 셋리스트 트랙 | 900곡 (60 × 15) | 900곡 | O |
| 공연 | 18개 (밴드당 3) | 18개 | O |
| 공연 포스터 | 18개 이상 | 19개 | O |
| 멤버 가용시간 | 120명 | 120명 | O |
| 스케줄 보드 | 대표 12개 | 12개 | O |

### 계정 규칙

| 항목 | 규칙 | 예시 |
|---|---|---|
| 로그인 ID | `member{n}` (변수명 그대로) | `member1` ~ `member120` |
| 이메일 | `member{n}@bandage.test` | `member1@bandage.test` |
| 비밀번호 | 전 계정 동일 | `12345678` |
| 표시 이름 | `멤버 {n}` (한글) | `멤버 1` |
| 가입 방식 | 자체 회원가입 (`POST /members/join`), 구글 로그인 미사용 | - |

> 밴드 n의 리더는 `member{(n-1)*20+1}` 입니다. 예: 밴드 1 리더 = member1, 밴드 6 리더 = member101.

## 2. 밴드별 데이터

| 밴드 | bandId | 소속 멤버 | 타밴드 교차가입 유입 | 셋리스트 | 트랙 | 공연 |
|---|---|---|---|---|---|---|
| 밴드 1 | `019fd5f2-1383-7bf4-8592-41ef828a8b93` | 20명 | 2명 | 10개 | 150곡 | 3개 |
| 밴드 2 | `019fd5f2-1bfa-790e-a7d2-2f3f75500259` | 20명 | 1명 | 10개 | 150곡 | 3개 |
| 밴드 3 | `019fd5f2-233d-7d7a-aab6-3c03142aa6e6` | 20명 | 2명 | 10개 | 150곡 | 3개 |
| 밴드 4 | `019fd5f2-2af7-70bf-994a-3e04e57fc8c2` | 20명 | 3명 | 10개 | 150곡 | 3개 |
| 밴드 5 | `019fd5f2-3252-7ddb-a2de-938fb0796a67` | 20명 | 2명 | 10개 | 150곡 | 3개 |
| 밴드 6 | `019fd5f2-395f-7e4d-aa80-4a5f046f3e09` | 20명 | 2명 | 10개 | 150곡 | 3개 |

- **교차 가입**: 각 밴드에서 2명(10%)이 타 밴드에 추가 가입 → 총 12명이 2개 이상 밴드 소속
- **밴드 프로필 이미지**: `poster-images` 이미지를 S3 presigned URL로 실제 업로드 후 `profileImg` 반영 (밴드 4·6은 5MB 초과로 최초 실패 → [F-05](#) 참조)

## 3. 셋리스트 구성

| 구분 | 개수 | 참여 구성 |
|---|---|---|
| 밴드 전체 참여 | 30개 (밴드당 5) | 해당 밴드 멤버 6명 |
| 개인 자격 멤버 포함 | 30개 (밴드당 5) | 해당 밴드 5명 + 타 밴드 개인 2명 |

| 규칙 | 내용 |
|---|---|
| 곡 수 | 셋리스트당 15곡 |
| 중복 곡 | 셋리스트당 2곡(약 13%)은 같은 밴드 내 다른 셋리스트와 공유 |
| 아티스트 풀 | Dream Theater 7곡, TOOL 7곡, 쏜애플 6곡, Metallica 7곡, Official HIGE DANdism 7곡 (총 34곡) |
| 세션 구성 | 80% 기본 4세션(VOCAL/GUITAR/BASS/DRUM), 20% 특이(커스텀 세션 추가 또는 2세션 축소) |
| 커스텀 세션 | KEYBOARD, SYNTH, VIOLIN, SAXOPHONE |

### 셋리스트 생성 파이프라인 (실제 서버 제약)

셋리스트는 직접 생성할 수 없고, 선곡(TrackSelection)을 거쳐 승격됩니다.

| 단계 | API | 필수 조건 |
|---|---|---|
| 1. 선곡 생성 | `POST /track-selections` | managerId, participantUserIds, bandIds 필수 |
| 2. 곡 추가 | `POST /track-selections/{id}/items` | title, artist, sessions 필수 |
| 3. 세션 지원 | `POST .../sessions/{sid}/applicants` | 참여자 본인 토큰 |
| 4. 세션 확정 | `PATCH .../sessions/{sid}/confirmations` | `{confirm:[], unconfirm:[]}` — 매니저만 |
| 5. 곡 선택 | `PATCH .../items/{id}/selection` | **모든 세션 확정 완료 필수** (미충족 시 `SETLIST_SELECTION_INCOMPLETE_SESSION`) |
| 6. 잠금 | `POST /track-selections/{id}/lock` | 매니저만 |
| 7. 셋리스트 승격 | `POST /setlists` | 선택된 트랙 1개 이상 (`SETLIST_NO_SELECTED_TRACK`) |

## 4. 공연 구성

| 공연 | 유형 | 참여 셋리스트 | 포스터 이미지 |
|---|---|---|---|
| 밴드 1 공연 1 (단독) | 단독 | 1개 | poster1.png |
| 밴드 1 공연 2 (2밴드 연합) | 2밴드 연합 | 2개 | poster2.jpg |
| 밴드 1 공연 3 (3밴드 연합) | 3밴드 연합 | 3개 | poster3.jpg |
| 밴드 2 공연 1 (단독) | 단독 | 1개 | poster7.jpg |
| 밴드 2 공연 2 (2밴드 연합) | 2밴드 연합 | 2개 | poster5.jpg, poster6.jpg |
| 밴드 2 공연 3 (3밴드 연합) | 3밴드 연합 | 3개 | poster1.png |
| 밴드 3 공연 1 (단독) | 단독 | 1개 | poster7.jpg |
| 밴드 3 공연 2 (2밴드 연합) | 2밴드 연합 | 2개 | poster1.png |
| 밴드 3 공연 3 (3밴드 연합) | 3밴드 연합 | 3개 | poster2.jpg |
| 밴드 4 공연 1 (단독) | 단독 | 1개 | poster3.jpg |
| 밴드 4 공연 2 (2밴드 연합) | 2밴드 연합 | 2개 | poster2.jpg |
| 밴드 4 공연 3 (3밴드 연합) | 3밴드 연합 | 3개 | poster5.jpg |
| 밴드 5 공연 1 (단독) | 단독 | 1개 | poster3.jpg |
| 밴드 5 공연 2 (2밴드 연합) | 2밴드 연합 | 2개 | poster7.jpg |
| 밴드 5 공연 3 (3밴드 연합) | 3밴드 연합 | 3개 | poster1.png |
| 밴드 6 공연 1 (단독) | 단독 | 1개 | poster2.jpg |
| 밴드 6 공연 2 (2밴드 연합) | 2밴드 연합 | 2개 | poster3.jpg |
| 밴드 6 공연 3 (3밴드 연합) | 3밴드 연합 | 3개 | poster4.jpg |

| 유형 | 개수 | 구성 |
|---|---|---|
| 단독 | 6개 | 해당 밴드 셋리스트만 |
| 2밴드 연합 | 6개 | 주최 밴드 + 초대 수락한 1개 밴드 |
| 3밴드 연합 | 6개 | 주최 밴드 + 초대 수락한 2개 밴드 |

**연합 공연 생성 순서** (서버 제약상 이 순서만 가능):

1. 주최 밴드 리더가 `POST /performances` — **자기 밴드 셋리스트만** 포함 가능
2. `POST /performances/{id}/invitations` 로 상대 밴드 리더 초대
3. 상대가 `PATCH .../invitations/{iid}?status=ACCEPTED` 수락
4. 수락한 상대 매니저가 `POST /performances/{id}/setlists/batch` 로 자기 셋리스트 추가

> 1단계에서 타 밴드 셋리스트를 넣으면 `403 SETLIST_FORBIDDEN` 입니다. (검증 I-09)

### 포스터 이미지 사용 현황

| 이미지 | 원본 크기 | 처리 | 사용 횟수 |
|---|---|---|---|
| poster1.png | 235 KB | 원본 그대로 | 4회 |
| poster2.jpg | 1.3 MB | 원본 그대로 | 4회 |
| poster3.jpg | 3.8 MB | 원본 그대로 | 4회 |
| poster4.jpg | 7.0 MB | 5MB 초과 → JPEG 리사이즈 | 1회 |
| poster5.jpg | 154 KB | 원본 그대로 | 2회 |
| poster6.jpg | 16.2 MB (poster6.png) | 5MB 초과 → JPEG 리사이즈 | 1회 |
| poster7.jpg | 1.9 MB | 원본 그대로 | 3회 |

`poster-images/` 7장 **전량이 최소 1회 이상 사용**되었습니다. 5MB를 초과하는 2장은 서버 한도(`FILE_SIZE_EXCEEDED`) 때문에 리사이즈 후 업로드했습니다.

## 5. 스케줄(가용시간) 데이터

30분 단위 슬롯(0~47), 적용 구간 `2026-08-10 ~ 2026-09-10` (약 한 달).

| 유형 | 비율 | 인원 | 주간 규칙 | 예외 |
|---|---|---|---|---|
| 일반 | 80% | 약 96명 | 평일 18:00~22:00 (슬롯 36~44), 주말 13:00~20:00 (슬롯 26~40) | 08-15 종일 차단 |
| 특이 - 심야만 | 20% 중 1/3 | 약 8명 | 매일 22:00~24:00 (슬롯 44~47) | 랜덤 AVAILABLE 1건 + BLOCKED 1건 |
| 특이 - 단일요일 종일 | 20% 중 1/3 | 약 8명 | 특정 1개 요일 00:00~24:00 (슬롯 0~47) | 동일 |
| 특이 - 가용시간 없음 | 20% 중 1/3 | 약 8명 | 규칙 없음 (빈 배열) | 동일 |

**스케줄 보드**: 대표 셋리스트 12개에 생성. 제약 조건 = 심야 제외, 최대 연속 180분, 가용 시간대 슬롯 18~44.

> Schedule 자동 배정 및 합주 생성은 미구현 기능이므로 테스트 대상에서 제외했습니다 (요구사항 명시).

## 6. 밴드 6 이탈 시나리오

마지막 밴드(밴드 6)에 대해 멤버·매니저 중도 이탈 케이스를 적용했습니다.

| # | 시나리오 | 대상 | API | 결과 |
|---|---|---|---|---|
| 1 | 멤버 자진 탈퇴 ×3 | member143/144/145 | `DELETE /bands/{id}/members/me` | 성공 (200) |
| 2 | 리더의 멤버 제명 ×3 | member146/147/148 | `DELETE /bands/{id}/members/{bandMemberId}` | 성공 (200) |
| 3 | 셋리스트 매니저 위임 | 142 → 143 | `PATCH /setlists/{id}/manager` | 성공 (필드명 `managerId`) |
| 4 | 연합공연 오너 위임 | 142 → 102 (타 밴드 리더) | `PATCH /performances/{id}/owner` | 성공 (필드명 `targetMemberId`) |
| 5 | 밴드 리더 위임 | 142 → 135 | `PATCH /bands/{id}/members/{bmId}/role` `{role:LEADER}` | 성공 |
| 6 | 위임 후 구 리더 탈퇴 | member142 | `DELETE /bands/{id}/members/me` | 성공 (200) |
| 7 | 리더 재위임 (재실행) | 143 → 96 | `PATCH /bands/{id}/members/{bmId}/role` | 성공 |
| 8 | 재위임 후 탈퇴 | member143 | `DELETE /bands/{id}/members/me` | 성공 (200) |

**최종 상태**: 밴드 6은 리더 1명 + 멤버 13명 (초기 20명 → 자진 탈퇴 3, 제명 3, 리더 이탈 2, 교차가입 유입 2 반영).

**확인된 동작**: 연합 공연의 오너가 타 밴드 리더에게 위임된 뒤 원 소유자가 밴드를 탈퇴해도 공연 데이터는 유지되며, 위임받은 매니저가 계속 관리할 수 있습니다. 오너 위임 대상은 해당 공연의 MANAGER(초대 수락자)여야 합니다.

## 7. 검증 케이스 전체 (138건)

`expect` = 기대 상태코드, `actual` = 실제 응답. FAIL 9건은 모두 장애지점보고서의 D-01~D-04에 해당합니다.


### A. 인증/회원가입

| ID | 케이스 | 기대 | 실제 | 결과 | 응답 코드 |
|---|---|---|---|---|---|
| A-01 | 회원가입 정상 | 200/201 | 200 | PASS | - |
| A-02 | 회원가입 중복 이메일 | 400/409 | 409 | PASS | DUPLICATE_EMAIL |
| A-03 | 회원가입 이메일 형식 오류 | 400 | 200 | **FAIL** | - |
| A-03b | 회원가입 @만 있는 이메일 | 400 | 200 | **FAIL** | - |
| A-03c | 회원가입 공백 포함 이메일 | 400 | 200 | **FAIL** | - |
| A-04 | 회원가입 필수값 누락 | 400 | 400 | PASS | INVALID_INPUT_VALUE |
| A-05 | 로그인 정상 | 200 | 200 | PASS | - |
| A-06 | 로그인 잘못된 비밀번호 | 400/401 | 400 | PASS | INVALID_PASSWORD |
| A-07 | 로그인 없는 계정 | 400/401/404 | 404 | PASS | MEMBER_NOT_FOUND |
| A-08 | 토큰 없이 보호 API | 401 | 401 | PASS | UNAUTHORIZED |
| A-09 | 잘못된 토큰 | 401 | 401 | PASS | UNAUTHORIZED |
| A-10 | 리프레시 토큰 누락 | 400/401 | 500 | **FAIL** | INTERNAL_SERVER_ERROR |
| A-11 | 비밀번호 변경 잘못된 현재값 | 400/401 | 400 | PASS | INVALID_INPUT_VALUE |
| A-12 | 구글 OAuth 잘못된 코드 | 400/401/500 | 400 | PASS | INVALID_INPUT_VALUE |
| A-13 | 카카오 OAuth 잘못된 코드 | 400/401/500 | 400 | PASS | INVALID_INPUT_VALUE |

### B. 회원

| ID | 케이스 | 기대 | 실제 | 결과 | 응답 코드 |
|---|---|---|---|---|---|
| B-01 | 내 정보 조회 | 200 | 200 | PASS | - |
| B-02 | 내 정보 수정 | 200 | 200 | PASS | - |
| B-03 | 타 회원 조회 | 200 | 200 | PASS | - |
| B-04 | 없는 회원 조회 | 404/400 | 404 | PASS | MEMBER_NOT_FOUND |
| B-05 | 회원 검색 | 200 | 200 | PASS | - |
| B-06 | 회원 검색 빈 결과 | 200 | 200 | PASS | - |
| B-07 | 내 지표 조회 | 200 | 200 | PASS | - |
| B-08 | 프로필 이미지 presign | 200 | 200 | PASS | - |
| B-09 | presign 5MB 초과 | 400/413 | 413 | PASS | FILE_SIZE_EXCEEDED |
| B-10 | presign 잘못된 확장자 | 400 | 400 | PASS | INVALID_FILE_EXTENSION |

### C. 밴드

| ID | 케이스 | 기대 | 실제 | 결과 | 응답 코드 |
|---|---|---|---|---|---|
| C-01 | 밴드 목록 | 200 | 200 | PASS | - |
| C-02 | 밴드 상세 | 200 | 200 | PASS | - |
| C-03 | 없는 밴드 조회 | 404 | 404 | PASS | BAND_NOT_FOUND |
| C-04 | 잘못된 UUID 형식 | 400 | 500 | **FAIL** | INTERNAL_SERVER_ERROR |
| C-05 | 밴드 검색 | 200 | 200 | PASS | - |
| C-06 | 내 밴드 목록 | 200 | 200 | PASS | - |
| C-07 | 밴드 이름 중복 생성 | 400/409 | 409 | PASS | DUPLICATE_BAND_NAME |
| C-08 | 밴드 이름 누락 | 400 | 400 | PASS | INVALID_INPUT_VALUE |
| C-09 | 밴드 수정(리더) | 200 | 200 | PASS | - |
| C-10 | 밴드 수정(외부인) | 403/404 | 403 | PASS | NOT_A_LEADER |
| C-11 | 밴드 멤버 목록 | 200 | 200 | PASS | - |
| C-12 | 밴드 삭제(외부인) | 403/404 | 403 | PASS | NOT_A_LEADER |
| C-13 | pageSize 상한 초과 | 400 | 400 | PASS | INVALID_INPUT_VALUE |
| C-14 | pageSize 0 | 400 | 400 | PASS | INVALID_INPUT_VALUE |
| C-15 | pageSize 누락(스펙상 required) | 200/400 | 200 | PASS | - |

### D. 밴드 가입 신청

| ID | 케이스 | 기대 | 실제 | 결과 | 응답 코드 |
|---|---|---|---|---|---|
| D-01 | 가입 신청 | 200/201 | 200 | PASS | - |
| D-02 | 중복 가입 신청 | 400/409 | 409 | PASS | DUPLICATE_BAND_APPLICATION |
| D-03 | 내 신청 목록 | 200 | 200 | PASS | - |
| D-04 | 특정 밴드 내 신청 | 200/404 | 200 | PASS | - |
| D-05 | 밴드 신청 목록(리더) | 200 | 200 | PASS | - |
| D-06 | 밴드 신청 목록(권한없음) | 403/404 | 403 | PASS | NOT_A_LEADER |
| D-07 | 신청 철회 | 200/400/404 | 200 | PASS | - |
| D-08 | 이미 멤버인 밴드에 신청 | 400/409 | 409 | PASS | BAND_MEMBER_ALREADY_EXISTS |

### E. 선곡(TrackSelection)

| ID | 케이스 | 기대 | 실제 | 결과 | 응답 코드 |
|---|---|---|---|---|---|
| E-01 | 내 선곡 목록 | 200 | 200 | PASS | - |
| E-02 | 선곡 상세 | 200 | 200 | PASS | - |
| E-03 | 선곡 상세(외부인) | 403/404 | 403 | PASS | SETLIST_MEETING_FORBIDDEN |
| E-04 | 선곡 항목 목록 | 200 | 200 | PASS | - |
| E-05 | 없는 선곡 조회 | 404 | 404 | PASS | SETLIST_MEETING_NOT_FOUND |
| E-06 | 선곡 제목 수정 | 200/400/409 | 200 | PASS | - |
| E-07 | lock 된 선곡에 항목 추가 | 400/409/403 | 409 | PASS | SETLIST_MEETING_LOCKED |
| E-08 | 중복 lock | 200/400/409 | 409 | PASS | SETLIST_MEETING_LOCKED |
| E-09 | unlock | 200/400/409 | 200 | PASS | - |
| E-10 | 세션 label 숫자 포함(패턴 위반) | 400 | 400 | PASS | SESSION_LABEL_NOT_ALPHABETIC |
| E-11 | 세션 빈 배열 | 400/200/201 | 200 | PASS | - |
| E-12 | artist 누락 | 400 | 400 | PASS | INVALID_INPUT_VALUE |

### F. 셋리스트

| ID | 케이스 | 기대 | 실제 | 결과 | 응답 코드 |
|---|---|---|---|---|---|
| F-01 | 내 셋리스트 목록 | 200 | 200 | PASS | - |
| F-02 | 셋리스트 상세 | 200 | 200 | PASS | - |
| F-03 | 셋리스트 트랙 목록 | 200 | 200 | PASS | - |
| F-04 | 셋리스트 참여자 | 200 | 200 | PASS | - |
| F-05 | 제목 부분검색 | 200 | 200 | PASS | - |
| F-06 | 제목 검색 빈 결과 | 200 | 200 | PASS | - |
| F-07 | 셋리스트 상세(외부인) | 403/404 | 403 | PASS | SETLIST_FORBIDDEN |
| F-08 | 셋리스트 수정 | 200 | 200 | PASS | - |
| F-09 | 없는 셋리스트 | 404 | 404 | PASS | SETLIST_NOT_FOUND |
| F-10 | trackSelectionId 없이 생성 | 400 | 400 | PASS | INVALID_INPUT_VALUE |
| F-11 | 존재하지 않는 선곡으로 생성 | 400/404 | 404 | PASS | SETLIST_MEETING_NOT_FOUND |
| F-12 | 트랙 단건 조회 | 200 | 200 | PASS | - |
| F-13 | 트랙 수정 | 200 | 200 | PASS | - |
| F-14 | 트랙 수정(외부인) | 403/404 | 403 | PASS | SETLIST_NOT_MANAGER |

### G. 스케줄 보드

| ID | 케이스 | 기대 | 실제 | 결과 | 응답 코드 |
|---|---|---|---|---|---|
| G-01 | 보드 목록 | 200 | 200 | PASS | - |
| G-02 | 보드 수정 | 200 | 200 | PASS | - |
| G-03 | 블록 생성(upsert) | 200/201 | 200 | PASS | - |
| G-04 | 블록 슬롯 범위 초과 | 400 | 400 | PASS | INVALID_INPUT_VALUE |
| G-05 | 블록 trackIds 빈 배열 | 400 | 400 | PASS | INVALID_INPUT_VALUE |
| G-06 | 블록 핀 설정 | 200 | 200 | PASS | - |
| G-07 | 블록 삭제 | 200/204 | 200 | PASS | - |
| G-08 | 보드 생성(외부인) | 403/404 | 403 | PASS | SETLIST_NOT_MANAGER |

### H. 가용시간

| ID | 케이스 | 기대 | 실제 | 결과 | 응답 코드 |
|---|---|---|---|---|---|
| H-01 | 가용시간 조회 | 200 | 200 | PASS | - |
| H-02 | 기간 조회 | 200 | 200 | PASS | - |
| H-03 | 슬롯 조회 | 200 | 200 | PASS | - |
| H-04 | 역전 기간(from>to) | 400/200 | 400 | PASS | AVAILABILITY_RANGE_INVALID |
| H-05 | 잘못된 날짜 형식 | 400 | 500 | **FAIL** | INTERNAL_SERVER_ERROR |
| H-06 | 슬롯 범위 초과 등록 | 400 | 400 | PASS | AVAILABILITY_INVALID |
| H-07 | start>end 슬롯 | 400/200 | 400 | PASS | AVAILABILITY_INVALID |
| H-08 | 잘못된 요일 | 400 | 400 | PASS | INVALID_INPUT_VALUE |

### I. 공연

| ID | 케이스 | 기대 | 실제 | 결과 | 응답 코드 |
|---|---|---|---|---|---|
| I-01 | 공연 목록 | 200 | 200 | PASS | - |
| I-02 | 공연 상세 | 200 | 200 | PASS | - |
| I-03 | 내 공연 | 200 | 200 | PASS | - |
| I-04 | 공연 검색 | 200 | 200 | PASS | - |
| I-05 | 밴드별 공연 | 200 | 200 | PASS | - |
| I-06 | 과거 시각 공연 생성 | 400 | 400 | PASS | INVALID_INPUT_VALUE |
| I-07 | durationMinutes 0 | 400 | 400 | PASS | INVALID_INPUT_VALUE |
| I-08 | 잘못된 날짜 형식 | 400 | 400 | PASS | INVALID_INPUT_VALUE |
| I-09 | 타 밴드 셋리스트로 생성 | 403/400 | 403 | PASS | SETLIST_FORBIDDEN |
| I-10 | 공연 수정(오너) | 200 | 200 | PASS | - |
| I-11 | 공연 수정(외부인) | 403/404 | 403 | PASS | NOT_A_PERFORMANCE_MANAGER |
| I-12 | 공연 셋리스트 트랙 | 200 | 200 | PASS | - |
| I-13 | 없는 공연 조회 | 404 | 404 | PASS | PERFORMANCE_NOT_FOUND |
| I-14 | 초대 목록 | 200 | 200 | PASS | - |
| I-15 | 내 초대 | 200 | 200 | PASS | - |
| I-16 | 없는 회원 초대 | 400/404 | 404 | PASS | MEMBER_NOT_FOUND |
| I-17 | 자기 자신 초대 | 400/409 | 409 | PASS | ALREADY_PERFORMANCE_MANAGER |
| I-18 | 공연 삭제(외부인) | 403/404 | 403 | PASS | NOT_A_PERFORMANCE_MANAGER |

### J. 포스터

| ID | 케이스 | 기대 | 실제 | 결과 | 응답 코드 |
|---|---|---|---|---|---|
| J-01 | 포스터 목록 | 200 | 200 | PASS | - |
| J-02 | 내 포스터 | 200 | 200 | PASS | - |
| J-03 | 공연별 포스터 | 200 | 200 | PASS | - |
| J-04 | 포스터 상세 | 200 | 200 | PASS | - |
| J-05 | 포스터 수정 | 200 | 200 | PASS | - |
| J-06 | 포스터 수정(외부인) | 403/404 | 403 | PASS | NOT_A_PERFORMANCE_MANAGER |
| J-07 | presign 권한 없음 | 403/404 | 403 | PASS | NOT_A_PERFORMANCE_MANAGER |
| J-08 | 없는 imageKey 등록 | 400/404 | 200 | **FAIL** | - |

### K. 합주(Jam)

| ID | 케이스 | 기대 | 실제 | 결과 | 응답 코드 |
|---|---|---|---|---|---|
| K-01 | 합주 목록 | 200 | 200 | PASS | - |
| K-02 | 내 합주 | 200 | 200 | PASS | - |
| K-03 | 내 합주 검색 | 200 | 200 | PASS | - |
| K-04 | 밴드별 합주 | 200 | 200 | PASS | - |
| K-05 | 없는 합주 조회 | 404 | 404 | PASS | JAM_NOT_FOUND |

### L. 알림

| ID | 케이스 | 기대 | 실제 | 결과 | 응답 코드 |
|---|---|---|---|---|---|
| L-01 | 알림 목록 | 200 | 200 | PASS | - |
| L-02 | 미읽음 수 | 200 | 200 | PASS | - |
| L-03 | 전체 읽음 | 200 | 200 | PASS | - |
| L-04 | 없는 알림 읽음(UUID) | 404 | 404 | PASS | NOTIFICATION_NOT_FOUND |
| L-05 | 알림 ID 타입 불일치(숫자) | 400 | 500 | **FAIL** | INTERNAL_SERVER_ERROR |
| L-06 | 타인 알림 읽음 | 403 | 403 | PASS | NOTIFICATION_FORBIDDEN |
| L-07 | 본인 알림 읽음 | 200 | 200 | PASS | - |

### M. 권한/위임

| ID | 케이스 | 기대 | 실제 | 결과 | 응답 코드 |
|---|---|---|---|---|---|
| M-01 | 역할 변경(외부인) | 403 | 403 | PASS | NOT_A_LEADER |
| M-01b | 역할 변경(리더, ADMIN->MEMBER) | 200 | 200 | PASS | - |
| M-01c | 잘못된 role 값 | 400 | 400 | PASS | INVALID_INPUT_VALUE |
| M-01d | bandMemberId 타입 불일치(숫자) | 400 | 500 | **FAIL** | INTERNAL_SERVER_ERROR |
| M-02 | 셋리스트 매니저 위임(비매니저) | 403/404 | 403 | PASS | SETLIST_NOT_MANAGER |
| M-03 | 선곡 매니저 위임(비매니저) | 403/404 | 403 | PASS | SETLIST_MEETING_NOT_MANAGER |
| M-04 | 공연 오너 위임(비오너) | 403/404 | 403 | PASS | NOT_A_PERFORMANCE_MANAGER |

### N. 페이징/커서

| ID | 케이스 | 기대 | 실제 | 결과 | 응답 코드 |
|---|---|---|---|---|---|
| N-01 | 커서 페이징 1p | 200 | 200 | PASS | - |
| N-02 | 커서 페이징 2p | 200 | 200 | PASS | - |
| N-03 | 잘못된 커서 | 400/200 | 400 | PASS | INVALID_INPUT_VALUE |

## 8. 재현 방법

```bash
cd scripts/e2e
python3 seed.py      # 데이터 생성 (약 8분, API 10,000여 건)
python3 fixup.py     # 5MB 초과 포스터 리사이즈본 재등록 + 위임 시나리오
python3 verify.py    # API 전수 검증 (135 케이스)
python3 cleanup.py   # 생성 밴드 전체 삭제
```

| 파일 | 역할 |
|---|---|
| `scripts/e2e/api.py` | HTTP 클라이언트 (stdlib만 사용, 외부 의존성 없음) |
| `scripts/e2e/spec.py` | 데이터 명세 — 곡 카탈로그, 밴드/멤버 배정, 세션 규칙 (고정 시드 `20260806`) |
| `scripts/e2e/seed.py` | 데이터 생성 본체 |
| `scripts/e2e/fixup.py` | 포스터 보정 + 위임 시나리오 |
| `scripts/e2e/verify.py` | API 전수 검증 + 엣지케이스 |
| `scripts/e2e/probe.py` | 파이프라인 축소 검증 (디버깅용) |
| `scripts/e2e/cleanup.py` | 생성 데이터 정리 |
| `scripts/e2e/restore.sh` | DB 초기화 후 전체 상태 복원 |

`spec.py`의 시드가 고정이므로 재실행 시 동일한 구성이 재현됩니다. 서버가 UUID/회원 ID를 새로 발급하므로 식별자 값 자체는 달라집니다.

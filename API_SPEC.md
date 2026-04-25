# API 기능 명세서

Base URL: `/api/v1`

모든 응답은 `ApiResponse<T>` 래퍼로 감싸져 반환됩니다.

```json
{
  "success": true|false,
  "message": "...",          // error 시에만
  "code": "ERROR_CODE",      // error 시에만 (ErrorCode enum 이름)
  "data": <T>,               // success 시 페이로드
  "fieldErrors": {            // 유효성 오류일 때만 (필드명 → 메시지)
    "name": "이름은 필수입니다."
  },
  "timestamp": "2026-04-25T12:00:00"
}
```

인증이 필요한 API는 `Authorization: Bearer {accessToken}` 헤더를 사용합니다.

---

## 0. 프론트 검증 리포트 대응 현황 (2026-04-25)

`bandage-fe/.taskmaster/reports/` 의 5개 검증 리포트에서 제기된 이슈에 대한 처리 현황입니다.

| # | 이슈 | 출처 | 상태 | 비고 |
|---|---|---|---|---|
| 1 | `BandCreateRequest.profileImg` non-null → optional | MVP 통합 §4 | ✅ 해소 | DTO를 `String?`로 변경 (3-1 참조) |
| 2 | `PerformanceUpdateRequest` 부분 업데이트 불가 | Performance §3-A | ✅ 해소 | 모든 필드 nullable + 서비스 partial update (6-4 참조) |
| 3 | `GET /api/v1/practices` 미구현 (405) | MVP 통합 §6, Practice §3-B | ✅ 해소 | 신규 엔드포인트 추가 (4-1-2 참조) |
| 4 | API_SPEC 인증 표기 vs 실제 인증 요구 불일치 | Band §3-A, Practice §3-C | ✅ 해소 | 본 문서의 모든 보호 엔드포인트는 "인증 필요"로 정렬됨 (`/auth/login`, `/auth/refresh`, `/members/join`만 공개) |
| 5 | `CursorResponse.nextCursor` 일관성 (`hasNext=false`일 때 null) | Band §3-C, Performance §3-C | ✅ 해소 | 모든 RepositoryImpl에서 `hasNext=false` 시 `nextCursor=null` |
| 6 | 인증(401) / 인가(403) 응답 코드 분리 | Auth §C, Band §3-B, Performance §3-B | ✅ 해소 (검증 완료) | `NOT_A_LEADER`, `NOT_A_PERFORMANCE_MANAGER` 모두 `HttpStatus.FORBIDDEN`로 매핑됨. JWT 무효/누락은 `JwtAuthenticationEntryPoint`가 401 + `WWW-Authenticate` 헤더 반환 |
| 7 | `ApiResponse`에 `code` / `fieldErrors` 추가 | Auth §D | ✅ 해소 | 본 문서 상단 응답 스키마 참조. `code`는 `ErrorCode` enum 이름. 유효성 오류 시 `fieldErrors` 맵 동봉 |
| 8 | `PerformanceDetailResponse` 요약 필드 (밴드명/합주 제목) | Performance §3-D | ✅ 해소 | `bandIds`, `practiceIds` → `bands: [{bandId, bandName}]`, `practices: [{practiceId, title, startAt}]`로 확장 (6-3 참조) |
| 9 | `GET /bands/me` 응답에 본인 역할 (`myRole`) 포함 | Band §3-D | ✅ 해소 (옵션 9-C) | 응답 항목 타입을 `MyBandInfoResponse`로 변경, `myRole: BandRole` 필드 추가 (3-3-1 참조) |

### 미해소 / 프론트 책임

- **`MemberInfoResponse.memberId` vs 프론트 기대 `id`** (MVP 통합 §3): 백엔드는 도메인 prefix 컨벤션(`bandId`, `practiceId`, `songId`, `bandMemberId`...)을 따르므로 `memberId` 유지. 프론트 타입을 `memberId`로 갱신 필요.

---

## 1. 인증 (Auth)

### 1-1. 회원 로그인
- **POST** `/api/v1/auth/login`
- **인증 불필요**
- **Request Body**
  ```json
  {
    "email": "member@google.com",
    "password": "pw1234"
  }
  ```
- **Response**
  ```json
  {
    "accessToken": "ey1234..."
  }
  ```
- **비고**: refresh 토큰은 `Set-Cookie` 헤더로 반환

---

### 1-2. 회원 로그아웃
- **DELETE** `/api/v1/auth/logout`
- **인증 필요**
- **비고**: Redis와 쿠키에 저장된 refresh 토큰을 만료 처리

---

### 1-3. 토큰 재발급
- **POST** `/api/v1/auth/refresh`
- **인증 불필요**
- **Request**: Cookie에 `refreshToken` 포함
- **Response**
  ```json
  {
    "accessToken": "ey1234..."
  }
  ```
- **비고**: 새로운 refresh 토큰은 `Set-Cookie` 헤더로 반환

---

### 1-4. 비밀번호 변경
- **PATCH** `/api/v1/auth/password`
- **인증 필요**
- **Request Body**
  ```json
  {
    "originalPassword": "originalPW123!",
    "newPassword": "newPW123!"
  }
  ```
- **비고**: 변경 후 refresh 토큰(쿠키) 만료 처리

---

## 2. 회원 (Member)

### 2-1. 회원 가입
- **POST** `/api/v1/members/join`
- **인증 불필요**
- **Request Body**
  ```json
  {
    "email": "member@google.com",
    "password": "pw1234",
    "name": "홍길동",
    "contact": "010-1234-5678"
  }
  ```
- **Response**
  ```json
  {
    "id": 1,
    "email": "member@gmail.com"
  }
  ```

---

### 2-2. 내 정보 조회
- **GET** `/api/v1/members/me`
- **인증 필요**
- **Response**: (구현 예정 — 현재 `Unit` 반환)

---

### 2-3. 내 정보 수정
- **PATCH** `/api/v1/members/me`
- **인증 필요**
- **Request Body**
  ```json
  {
    "name": "홍길동",
    "contact": "010-1234-5678"
  }
  ```
- **비고**: 각 필드는 nullable, 전달된 필드만 업데이트

---

### 2-4. 회원 탈퇴
- **DELETE** `/api/v1/members/me`
- **인증 필요**
- **비고**: 회원 정보 삭제(soft delete) 및 로그아웃 처리 (쿠키 만료)

---

## 3. 밴드 (Band)

### 3-1. 밴드 생성
- **POST** `/api/v1/bands`
- **인증 필요**
- **Request Body**
  ```json
  {
    "name": "TuNA",
    "description": "성균관대학교 문과대 락밴드 TuNA 입니다.",
    "profileImg": "url"
  }
  ```
  - `profileImg`: optional
- **Response**
  ```json
  {
    "bandId": "550e8400-e29b-41d4-a716-446655440000",
    "bandName": "TuNA"
  }
  ```
- **비고**: 생성자는 자동으로 LEADER 역할로 등록

---

### 3-2. 밴드 단건 조회
- **GET** `/api/v1/bands/{bandId}`
- **인증 필요**
- **Path Variable**: `bandId` (UUID)
- **Response**
  ```json
  {
    "bandId": "550e8400-e29b-41d4-a716-446655440000",
    "bandName": "TuNA",
    "description": "성균관대학교 문과대 락밴드 TuNA 입니다.",
    "profileImg": "image_url"
  }
  ```

---

### 3-3. 밴드 목록 조회 (커서 페이징)
- **GET** `/api/v1/bands`
- **인증 필요**
- **Query Parameters**
  | 파라미터 | 타입 | 필수 | 기본값 | 설명 |
  |---------|------|------|--------|------|
  | `lastId` | UUID | N | - | 이전 페이지 마지막 밴드 ID |
  | `pageSize` | Int (1~100) | N | 10 | 페이지 크기 |
- **Response**: `CursorResponse<BandInfoResponse, UUID>`

---

### 3-3-1. 내 밴드 목록 조회 (커서 페이징)
- **GET** `/api/v1/bands/me`
- **인증 필요**
- **Query Parameters**
  | 파라미터 | 타입 | 필수 | 기본값 | 설명 |
  |---------|------|------|--------|------|
  | `lastId` | UUID | N | - | 이전 페이지 마지막 밴드 ID |
  | `pageSize` | Int (1~100) | N | 10 | 페이지 크기 |
- **Response**: `CursorResponse<MyBandInfoResponse, UUID>`
  ```json
  {
    "content": [
      {
        "bandId": "550e8400-e29b-41d4-a716-446655440000",
        "bandName": "TuNA",
        "description": "...",
        "profileImg": "url",
        "myRole": "LEADER"
      }
    ],
    "nextCursor": null,
    "hasNext": false
  }
  ```
- **비고**: 현재 로그인한 회원이 소속된 밴드만 조회. 응답 항목에 본인의 밴드 내 역할(`myRole`)이 포함되어 별도 멤버 목록 조회 없이도 권한 판정 가능. `myRole` enum — `LEADER` | `ADMIN` | `MEMBER`

---

### 3-3-2. 밴드 검색 (커서 페이징)
- **GET** `/api/v1/bands/search`
- **인증 필요**
- **Query Parameters**
  | 파라미터 | 타입 | 필수 | 기본값 | 설명 |
  |---------|------|------|--------|------|
  | `keyword` | String | Y | - | 검색 키워드 (밴드 이름) |
  | `lastId` | UUID | N | - | 이전 페이지 마지막 밴드 ID |
  | `pageSize` | Int (1~100) | N | 10 | 페이지 크기 |
- **Response**: `CursorResponse<BandInfoResponse, UUID>`
- **비고**: 밴드 이름에 `keyword`가 포함된 밴드를 대소문자 구분 없이 부분 매칭 조회

---

### 3-4. 밴드 멤버 단건 조회
- **GET** `/api/v1/bands/{bandId}/members/{bandMemberId}`
- **인증 필요**
- **Path Variables**: `bandId` (UUID), `bandMemberId` (UUID)
- **Response**
  ```json
  {
    "bandMemberId": "550e8400-e29b-41d4-a716-446655440000",
    "memberId": 1,
    "role": "MEMBER"
  }
  ```
- **비고**: `role` enum — `LEADER` | `ADMIN` | `MEMBER`

---

### 3-5. 밴드 멤버 목록 조회 (커서 페이징)
- **GET** `/api/v1/bands/{bandId}/members`
- **인증 필요**
- **Path Variable**: `bandId` (UUID)
- **Query Parameters**: `lastId` (UUID, optional), `pageSize` (Int 1~100, 기본값 10)
- **Response**: `CursorResponse<BandMemberInfoResponse, UUID>`

---

### 3-6. 밴드 가입 신청
- **POST** `/api/v1/bands/{bandId}/applications`
- **인증 필요**
- **Path Variable**: `bandId` (UUID)

---

### 3-7. 밴드 가입 신청 목록 조회 (커서 페이징)
- **GET** `/api/v1/bands/{bandId}/applications`
- **인증 필요** (리더/관리자)
- **Path Variable**: `bandId` (UUID)
- **Query Parameters**
  | 파라미터 | 타입 | 필수 | 기본값 | 설명 |
  |---------|------|------|--------|------|
  | `lastId` | UUID | N | - | 이전 페이지 마지막 신청 ID |
  | `pageSize` | Int (1~100) | N | 10 | 페이지 크기 |
  | `status` | ApplicationStatus | N | `PENDING` | 신청 상태 필터 |
- **비고**: `status` enum — `PENDING` | `APPROVED` | `REJECTED` | `WITHDRAWN` | `LEAVED`
- **Response**: `CursorResponse<BandApplicationInfoResponse, UUID>`
  ```json
  {
    "bandApplicationId": "550e8400-...",
    "memberId": 1,
    "status": "PENDING"
  }
  ```

---

### 3-8. 밴드 가입 신청 철회 (본인)
- **PATCH** `/api/v1/bands/{bandId}/applications/me`
- **인증 필요**
- **Path Variable**: `bandId` (UUID)
- **비고**: `PENDING` 상태의 본인 신청만 철회 가능

---

### 3-9. 밴드 가입 신청 승인/거절 (리더)
- **PATCH** `/api/v1/bands/{bandId}/applications/{bandApplicationId}`
- **인증 필요** (리더)
- **Path Variables**: `bandId` (UUID), `bandApplicationId` (UUID)
- **Query Parameter**: `status` (`APPROVED` | `REJECTED`)

---

### 3-10. 밴드 리더 권한 위임
- **PATCH** `/api/v1/bands/{bandId}/members/{bandMemberId}/role`
- **인증 필요** (리더)
- **Path Variables**: `bandId` (UUID), `bandMemberId` (UUID)
- **비고**: 현재 리더 권한을 지정 멤버에게 양도

---

### 3-11. 밴드 탈퇴
- **DELETE** `/api/v1/bands/{bandId}/members/me`
- **인증 필요**
- **Path Variable**: `bandId` (UUID)

---

## 4. 합주 (Practice)

### 4-1. 합주 생성
- **POST** `/api/v1/practices`
- **인증 필요**
- **Request Body**
  ```json
  {
    "title": "TuNA 정기공연 1주차 합주",
    "song": "550e8400-e29b-41d4-a716-446655440000",
    "venue": "홍대 스튜디오",
    "startAt": "2026-03-15 18:00",
    "durationMinutes": 60
  }
  ```
  - `title`: optional
  - `venue`: optional
  - `startAt` 형식: `yyyy-MM-dd HH:mm` (Asia/Seoul 기준)
- **Response**
  ```json
  {
    "practiceId": "550e8400-e29b-41d4-a716-446655440000",
    "practiceTitle": "TuNA 정기공연 1주차 합주"
  }
  ```

---

### 4-1-2. 합주 목록 조회 (커서 페이징)
- **GET** `/api/v1/practices`
- **인증 필요**
- **Query Parameters**
  | 파라미터 | 타입 | 필수 | 기본값 | 설명 |
  |---------|------|------|--------|------|
  | `bandId` | UUID | N | - | 특정 밴드 멤버가 참여 중인 합주만 조회 |
  | `lastId` | UUID | N | - | 이전 페이지 마지막 합주 ID |
  | `pageSize` | Int (1~100) | N | 10 | 페이지 크기 |
- **Response**: `CursorResponse<PracticeListResponse, UUID>`
- **비고**: `bandId` 미제공 시 모든 합주 조회. `bandId` 제공 시 해당 밴드 소속 멤버 중 1명 이상이 참여하는 합주만 반환 (밴드에 멤버가 없으면 빈 목록).

---

### 4-1-1. 내 합주 목록 조회 (커서 페이징)
- **GET** `/api/v1/practices/me`
- **인증 필요**
- **Query Parameters**
  | 파라미터 | 타입 | 필수 | 기본값 | 설명 |
  |---------|------|------|--------|------|
  | `lastId` | UUID | N | - | 이전 페이지 마지막 합주 ID |
  | `pageSize` | Int (1~100) | N | 10 | 페이지 크기 |
- **Response**: `CursorResponse<PracticeListResponse, UUID>`
  ```json
  {
    "practiceId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "TuNA 정기공연 1주차 합주",
    "startAt": "2026-03-15 18:00",
    "durationMinutes": 60,
    "venue": "홍대 스튜디오"
  }
  ```
- **비고**: 현재 로그인한 회원이 참여 중인 합주만 조회

---

### 4-1-3. 내 합주 검색 (커서 페이징)
- **GET** `/api/v1/practices/me/search`
- **인증 필요**
- **Query Parameters**
  | 파라미터 | 타입 | 필수 | 기본값 | 설명 |
  |---------|------|------|--------|------|
  | `keyword` | String | Y | - | 검색 키워드 (합주 타이틀 또는 곡 제목) |
  | `lastId` | UUID | N | - | 이전 페이지 마지막 합주 ID |
  | `pageSize` | Int (1~100) | N | 10 | 페이지 크기 |
- **Response**: `CursorResponse<PracticeListResponse, UUID>`
- **비고**: 본인이 참여 중인 합주 중 합주 타이틀 또는 합주곡 제목에 `keyword`가 포함된 합주만 대소문자 구분 없이 조회

---

### 4-2. 합주 상세 조회
- **GET** `/api/v1/practices/{practiceId}`
- **인증 필요**
- **Path Variable**: `practiceId` (UUID)
- **Response**
  ```json
  {
    "practiceId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "TuNA 정기공연 1주차 합주",
    "venue": "홍대 스튜디오",
    "startAt": "2026-03-15 18:00",
    "durationMinutes": 60,
    "song": {
      "songId": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Stairway to Heaven",
      "artist": "Led Zeppelin"
    },
    "sessions": [
      {
        "sessionId": "550e8400-e29b-41d4-a716-446655440000",
        "label": "Guitar 2",
        "type": "GUITAR",
        "participant": null
      }
    ],
    "participants": [
      {
        "participantId": "550e8400-e29b-41d4-a716-446655440000",
        "memberId": 1
      }
    ]
  }
  ```

---

### 4-3. 합주 세션 생성
- **POST** `/api/v1/practices/{practiceId}/sessions`
- **인증 필요**
- **Path Variable**: `practiceId` (UUID)
- **Request Body**
  ```json
  {
    "label": "Guitar 2",
    "type": "GUITAR"
  }
  ```
  - `type` enum — `VOCAL` | `CHORUS` | `GUITAR` | `BASS` | `DRUM` | `PERCUSSION` | `SYNTH` | `ETC` (기본값: `ETC`)
- **Response**
  ```json
  {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "label": "Guitar 2",
    "type": "GUITAR",
    "participant": null
  }
  ```

---

### 4-4. 합주 세션 삭제
- **DELETE** `/api/v1/practices/{practiceId}/sessions/{sessionId}`
- **인증 필요**
- **Path Variables**: `practiceId` (UUID), `sessionId` (UUID)

---

### 4-5. 합주 멤버 추가
- **POST** `/api/v1/practices/{practiceId}/participants`
- **인증 필요**
- **Path Variable**: `practiceId` (UUID)
- **Request Body**
  ```json
  {
    "memberId": 1
  }
  ```
- **Response**
  ```json
  {
    "participantId": "550e8400-e29b-41d4-a716-446655440000",
    "memberId": 1
  }
  ```

---

### 4-6. 합주 세션 멤버 지정 (본인)
- **PATCH** `/api/v1/practices/{practiceId}/sessions/{sessionId}/assignment`
- **인증 필요**
- **Path Variables**: `practiceId` (UUID), `sessionId` (UUID)
- **비고**: 본인을 해당 세션에 배정

---

### 4-7. 합주 세션 멤버 지정 취소 (본인)
- **DELETE** `/api/v1/practices/{practiceId}/sessions/{sessionId}/assignment`
- **인증 필요**
- **Path Variables**: `practiceId` (UUID), `sessionId` (UUID)
- **비고**: 본인의 세션 배정 취소

---

### 4-8. 합주 일정 변경
- **PATCH** `/api/v1/practices/{practiceId}/schedule`
- **인증 필요**
- **Path Variable**: `practiceId` (UUID)
- **Request Body**
  ```json
  {
    "startAt": "2026-03-15 18:00",
    "durationMinutes": 90
  }
  ```
  - `startAt` 형식: `yyyy-MM-dd HH:mm` (Asia/Seoul 기준)

---

### 4-9. 합주 장소 변경
- **PATCH** `/api/v1/practices/{practiceId}/venue`
- **인증 필요**
- **Path Variable**: `practiceId` (UUID)
- **Request Body**
  ```json
  {
    "venue": "Club FF"
  }
  ```

---

### 4-10. 합주 삭제
- **DELETE** `/api/v1/practices/{practiceId}`
- **인증 필요**
- **Path Variable**: `practiceId` (UUID)

---

## 5. 합주곡 (Practice Song)

### 5-1. 합주곡 검색 (Song)
- **GET** `/api/v1/practice-songs/search`
- **인증 필요**
- **Query Parameters**
  | 파라미터 | 타입 | 필수 | 기본값 | 설명 |
  |---------|------|------|--------|------|
  | `keyword` | String | Y | - | 검색 키워드 (곡 제목 또는 아티스트) |
- **Response**: `List<Song>` — 외부 시스템 곡 정보 (DB 엔티티로 관리되지 않는 전송용 DTO)
  ```json
  [
    {
      "title": "Vicarious",
      "artist": "Tool",
      "album": "10,000 Days",
      "duration": 426,
      "refLink": null
    }
  ]
  ```
- **비고**:
  - 외부 음원 검색 API / gRPC 연동 전 임시 mock 응답 (Tool 곡 고정 반환). 추후 외부 시스템과 연동 예정.
  - 응답 배열의 각 `Song` 객체는 그대로 [5-3](#5-3-합주곡-생성-song-객체--외부-시스템-결과-사용)의 `song` 필드 또는 [5-5](#5-5-합주곡-upsert-song-객체)의 Request Body로 전달 가능 (재가공 불필요).

---

### 5-2. 합주곡 생성 (필드 입력 — 자작곡 등)
- **POST** `/api/v1/practice-songs`
- **인증 필요**
- **Request Body**
  ```json
  {
    "practiceId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "자작곡 No.1",
    "artist": "TuNA",
    "album": "TuNA Demo",
    "duration": 300,
    "refLink": null
  }
  ```
  - `refLink`: optional
- **Response**: `PracticeSongResponse`
  ```json
  {
    "songId": "550e8400-e29b-41d4-a716-446655440001",
    "title": "자작곡 No.1",
    "artist": "TuNA",
    "album": "TuNA Demo",
    "duration": 300,
    "refLink": null
  }
  ```
- **비고**: 자작곡 등 외부 API 연동이 필요 없는 곡을 사용자가 각 필드를 직접 입력하여 합주곡으로 생성. 생성된 합주곡은 `practiceId`의 합주에 1:1 바인딩.

---

### 5-3. 합주곡 생성 (Song 객체 — 외부 시스템 결과 사용)
- **POST** `/api/v1/practice-songs/from-song`
- **인증 필요**
- **Request Body**
  ```json
  {
    "practiceId": "550e8400-e29b-41d4-a716-446655440000",
    "song": {
      "title": "Vicarious",
      "artist": "Tool",
      "album": "10,000 Days",
      "duration": 426,
      "refLink": null
    }
  }
  ```
- **Response**: `PracticeSongResponse` (5-2와 동일 구조)
- **비고**: 외부 시스템에서 받아온 Song 객체를 그대로 사용하여 합주곡을 생성. 생성된 합주곡은 `practiceId`의 합주에 1:1 바인딩. `song` 필드는 [5-1](#5-1-합주곡-검색-song) 응답의 항목을 재가공 없이 그대로 사용 가능.

---

### 5-4. 합주곡 부분 수정
- **PATCH** `/api/v1/practice-songs/{songId}`
- **인증 필요**
- **Path Variable**: `songId` (UUID)
- **Request Body** (모든 필드 optional, 전달된 필드만 업데이트)
  ```json
  {
    "title": "Vicarious",
    "artist": "Tool",
    "album": "10,000 Days",
    "duration": 426,
    "refLink": "https://www.youtube.com/watch?v=example"
  }
  ```
- **Response**: `PracticeSongResponse`

---

### 5-5. 합주곡 Upsert (Song 객체)
- **PUT** `/api/v1/practice-songs/{songId}`
- **인증 필요**
- **Path Variable**: `songId` (UUID)
- **Request Body**: `Song` (전체 필드)
  ```json
  {
    "title": "Vicarious",
    "artist": "Tool",
    "album": "10,000 Days",
    "duration": 426,
    "refLink": "https://www.youtube.com/watch?v=example"
  }
  ```
- **Response**: `PracticeSongResponse`
- **비고**: Song 객체의 값으로 모든 필드를 갱신. Request Body는 [5-1](#5-1-합주곡-검색-song) 응답의 항목을 재가공 없이 그대로 사용 가능.

---

### 5-6. 합주곡 참조 링크 등록/수정 (Upsert)
- **PUT** `/api/v1/practice-songs/{songId}/ref-link`
- **인증 필요**
- **Path Variable**: `songId` (UUID)
- **Request Body**
  ```json
  {
    "refLink": "https://www.youtube.com/watch?v=example"
  }
  ```

---

### 5-7. 합주곡 참조 링크 삭제
- **DELETE** `/api/v1/practice-songs/{songId}/ref-link`
- **인증 필요**
- **Path Variable**: `songId` (UUID)

---

## 6. 공연 (Performance)

### 6-1. 공연 생성
- **POST** `/api/v1/performances`
- **인증 필요**
- **Request Body**
  ```json
  {
    "title": "TuNA 정기공연",
    "bandIds": ["550e8400-e29b-41d4-a716-446655440000"],
    "startAt": "2026-06-15 18:00",
    "durationMinutes": 120,
    "venue": "Club FF"
  }
  ```
  - `bandIds`: optional (기본값 빈 배열)
  - `venue`: optional
  - `startAt` 형식: `yyyy-MM-dd HH:mm` (Asia/Seoul 기준)
- **Response**
  ```json
  {
    "performanceId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "TuNA 정기공연"
  }
  ```
- **비고**: 생성자는 자동으로 PerformanceManager로 등록

---

### 6-2. 공연 목록 조회 (커서 페이징)
- **GET** `/api/v1/performances`
- **인증 필요**
- **Query Parameters**
  | 파라미터 | 타입 | 필수 | 기본값 | 설명 |
  |---------|------|------|--------|------|
  | `bandId` | UUID | N | - | 특정 밴드 소속 공연만 조회 |
  | `lastId` | UUID | N | - | 이전 페이지 마지막 공연 ID |
  | `pageSize` | Int (1~100) | N | 10 | 페이지 크기 |
- **Response**: `CursorResponse<PerformanceListResponse, UUID>`
  ```json
  {
    "performanceId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "TuNA 정기공연",
    "startAt": "2026-06-15 18:00",
    "durationMinutes": 120,
    "venue": "Club FF"
  }
  ```

---

### 6-2-1. 내 공연 목록 조회 (커서 페이징)
- **GET** `/api/v1/performances/me`
- **인증 필요**
- **Query Parameters**
  | 파라미터 | 타입 | 필수 | 기본값 | 설명 |
  |---------|------|------|--------|------|
  | `lastId` | UUID | N | - | 이전 페이지 마지막 공연 ID |
  | `pageSize` | Int (1~100) | N | 10 | 페이지 크기 |
- **Response**: `CursorResponse<PerformanceListResponse, UUID>`
- **비고**: 현재 로그인한 회원이 속한 밴드가 참여하는 모든 공연 조회 (소속 밴드가 없으면 빈 목록 반환)

---

### 6-2-2. 공연 검색 (커서 페이징)
- **GET** `/api/v1/performances/search`
- **인증 필요**
- **Query Parameters**
  | 파라미터 | 타입 | 필수 | 기본값 | 설명 |
  |---------|------|------|--------|------|
  | `keyword` | String | Y | - | 검색 키워드 (공연 이름) |
  | `lastId` | UUID | N | - | 이전 페이지 마지막 공연 ID |
  | `pageSize` | Int (1~100) | N | 10 | 페이지 크기 |
- **Response**: `CursorResponse<PerformanceListResponse, UUID>`
- **비고**: 공연 제목에 `keyword`가 포함된 공연을 대소문자 구분 없이 부분 매칭 조회

---

### 6-3. 공연 상세 조회
- **GET** `/api/v1/performances/{performanceId}`
- **인증 필요**
- **Path Variable**: `performanceId` (UUID)
- **Response**: `PerformanceDetailResponse`
  ```json
  {
    "performanceId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "TuNA 정기공연",
    "startAt": "2026-06-15 18:00",
    "durationMinutes": 120,
    "venue": "Club FF",
    "bands": [
      { "bandId": "550e8400-e29b-41d4-a716-446655440000", "bandName": "TuNA" }
    ],
    "managerIds": [1],
    "practices": [
      {
        "practiceId": "550e8400-e29b-41d4-a716-446655440001",
        "title": "TuNA 정기공연 1주차 합주",
        "startAt": "2026-06-01 18:00"
      }
    ]
  }
  ```
- **비고**: 기존 `bandIds` / `practiceIds` 평면 배열은 각각 `bands` / `practices` 요약 객체 배열로 확장됨. 프론트가 별도 호출 없이 밴드 이름 / 합주 제목 / 합주 시작시각을 표시 가능.

---

### 6-4. 공연 정보 수정
- **PATCH** `/api/v1/performances/{performanceId}`
- **인증 필요** (PerformanceManager)
- **Path Variable**: `performanceId` (UUID)
- **Request Body** (모든 필드 optional, 전달된 필드만 갱신)
  ```json
  {
    "title": "TuNA 정기공연 (수정)",
    "startAt": "2026-06-15 19:00",
    "durationMinutes": 90,
    "venue": "Club FF"
  }
  ```
  - 모든 필드 optional. 미전달 필드는 기존 값 유지.

---

### 6-5. 공연 합주 추가 (신규 생성)
- **POST** `/api/v1/performances/{performanceId}/practices`
- **인증 필요** (PerformanceManager)
- **Path Variable**: `performanceId` (UUID)
- **Request Body**
  ```json
  {
    "title": "TuNA 정기공연 합주",
    "songId": "550e8400-e29b-41d4-a716-446655440000",
    "startAt": "2026-06-01 18:00",
    "durationMinutes": 60,
    "venue": "홍대 스튜디오"
  }
  ```
  - `title`: optional (미입력 시 곡 제목으로 대체)
  - `venue`: optional
- **Response**
  ```json
  {
    "performancePracticeId": "550e8400-e29b-41d4-a716-446655440000",
    "practiceId": "550e8400-e29b-41d4-a716-446655440001"
  }
  ```
- **비고**: 빈 합주를 즉시 생성하여 공연에 연결

---

### 6-6. 공연 합주 일괄 추가 (기존 합주 연결)
- **POST** `/api/v1/performances/{performanceId}/practices/batch`
- **인증 필요** (PerformanceManager)
- **Path Variable**: `performanceId` (UUID)
- **Request Body**
  ```json
  {
    "practiceIds": [
      "550e8400-e29b-41d4-a716-446655440000",
      "550e8400-e29b-41d4-a716-446655440001"
    ]
  }
  ```
- **Response**: `List<PerformancePracticeResponse>`
  ```json
  [
    {
      "performancePracticeId": "550e8400-e29b-41d4-a716-446655440000",
      "practiceId": "550e8400-e29b-41d4-a716-446655440001"
    }
  ]
  ```

---

### 6-7. 공연 합주 삭제
- **DELETE** `/api/v1/performances/{performanceId}/practices/{practiceId}`
- **인증 필요** (PerformanceManager)
- **Path Variables**: `performanceId` (UUID), `practiceId` (UUID)
- **비고**: 공연과 합주의 연결을 제거 (합주 자체는 삭제되지 않음)

---

### 6-8. 공연 삭제
- **DELETE** `/api/v1/performances/{performanceId}`
- **인증 필요** (PerformanceManager)
- **Path Variable**: `performanceId` (UUID)
- **비고**: 연관된 합주도 함께 삭제

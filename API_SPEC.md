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

## 0. 프론트 검증 리포트 대응 현황 (2026-04-26)

`bandage-fe/.taskmaster/reports/` 의 5개 검증 리포트 + `API_ISSUE_REPORT.md` (mvp-1-fix-v3 Task 8) 에서 제기된 이슈에 대한 처리 현황입니다.

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
| 10 | `PerformanceCreateRequest.bandIds` non-null 차단 (T6) | API_ISSUE_REPORT §4-1 | ✅ 해소 | `bandIds: List<UUID>? = null` 로 변경, 미전달 시 빈 배열로 보정 (6-1 참조) |
| 11 | Practice ↔ PracticeSong 닭-달걀 (FE-API-020) | API_ISSUE_REPORT §4-2 | ✅ 해소 (§5-2 안 채택) | PracticeSong 생성 API의 `practiceId` 옵셔널화 → FE는 PracticeSong 선행 생성 후 응답 `songId` 를 `/practices` 에 전달 (5-2/5-3/4-1 참조) |
| 12 | 입력 검증 메시지 정제 (T3, T6) | API_ISSUE_REPORT §4-3 | ✅ 해소 | `HttpMessageNotReadableException` 핸들러가 `KotlinInvalidNullException`/`InvalidFormatException` 을 한국어 메시지 + `fieldErrors` 로 매핑 |
| 13 | 과거 시각 startAt 검증 (T17) | API_ISSUE_REPORT §4-4 | ✅ 해소 | Practice/Performance 생성·수정·합주 일정 변경 DTO 의 `startAt` 에 `@Future` 적용 (4-1/4-8/6-1/6-4 참조) |

### 추가 반영 (2026-04-27)

| # | 이슈 | 출처 | 상태 | 비고 |
|---|---|---|---|---|
| 14 | `MemberInfoResponse.id` alias / `profileImg` 노출 | MVP 통합 §3 | ✅ 해소 | `id` 와 `memberId` 동시 노출 (alias), `profileImg` 필드 추가 (2-2 참조) |
| 15 | `BandMemberInfoResponse` 에 회원 이름/프로필 이미지 포함 | FE-API-012 | ✅ 해소 | `name`, `profileImg` 추가 (3-4/3-5 참조) |
| 16 | `BandApplicationInfoResponse` 에 신청자 정보 포함 | FE-API-013 | ✅ 해소 | `applicantName`, `applicantProfileImg`, `appliedAt` 추가 (3-7 참조) |
| 17 | 회원 메트릭 (밴드 수 / 다가오는 합주·공연 수 / 세션 수) | FE-API-014 | ✅ 해소 | `GET /members/me/metrics` 신규 (2-5 참조). 네이밍은 Stat → Metrics 로 통일 |
| 18 | 회원 검색 (이름/이메일 부분 일치) | FE-API-032 | ✅ 해소 | `GET /members/search?q=` 신규, 본인 제외, 최대 20건 (2-6 참조) |
| 19 | 밴드 정보 수정 / 삭제 / 멤버 강퇴 / 역할 변경 | FE-API-022/023, §8-2 | ✅ 해소 | 3-12 ~ 3-15 참조 |
| 20 | 공연 참여 밴드 일괄 추가/단건 제거 | FE-API-017 | ✅ 해소 | 6-9 / 6-10 참조 |

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
- **Response**: `MemberInfoResponse`
  ```json
  {
    "id": 1,
    "memberId": 1,
    "email": "member@google.com",
    "name": "홍길동",
    "contact": "010-1234-5678",
    "profileImg": "https://cdn/...jpg"
  }
  ```
- **비고**: `id` 는 `memberId` 의 프론트 호환 alias 필드. `profileImg` 는 미설정 시 `null`.

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

### 2-5. 내 메트릭 조회
- **GET** `/api/v1/members/me/metrics`
- **인증 필요**
- **Response**: `MemberMetricsResponse`
  ```json
  {
    "bandCount": 3,
    "upcomingPracticeCount": 2,
    "upcomingPerformanceCount": 1,
    "sessionCount": 5
  }
  ```
- **비고**: 본인이 소속된 밴드 수, 본인이 참여한 다가오는 합주 수(`startAt > now`), 본인 소속 밴드의 다가오는 공연 수(`startAt > now`), 본인이 참여 중인 합주 세션 수. 도메인 간 의존(`Band`/`Practice`/`Performance`)이 있어 `MemberMetricsFacade` 에서 집계.

---

### 2-6. 회원 검색
- **GET** `/api/v1/members/search`
- **인증 필요**
- **Query Parameters**
  | 파라미터 | 타입 | 필수 | 기본값 | 설명 |
  |---------|------|------|--------|------|
  | `q` | String | N | `""` | 검색 키워드 (이름 또는 이메일) |
- **Response**: `List<MemberSearchItemResponse>`
  ```json
  [
    {
      "memberId": 1,
      "name": "홍길동",
      "email": "user@bandage.test",
      "profileImg": "https://cdn/...jpg"
    }
  ]
  ```
- **비고**: 이름/이메일 부분 일치 (대소문자 구분 X). 본인은 결과에서 제외. 최대 20건.

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
- **Response**: `BandMemberInfoResponse`
  ```json
  {
    "bandMemberId": "550e8400-e29b-41d4-a716-446655440000",
    "memberId": 1,
    "role": "MEMBER",
    "name": "홍길동",
    "profileImg": "https://cdn/...jpg"
  }
  ```
- **비고**: `role` enum — `LEADER` | `ADMIN` | `MEMBER`. `name`/`profileImg` 는 회원 조회 후 매핑 (FE-API-012).

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
    "status": "PENDING",
    "applicantName": "홍길동",
    "applicantProfileImg": "https://cdn/...jpg",
    "appliedAt": "2026-04-26T12:34:56"
  }
  ```
- **비고**: `applicantName`/`applicantProfileImg` 는 신청자 회원 조회 후 매핑 (FE-API-013). `appliedAt` 은 신청 생성 시각.

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

### 3-10. 밴드 멤버 역할 변경 / 리더 위임
- **PATCH** `/api/v1/bands/{bandId}/members/{bandMemberId}/role`
- **인증 필요** (리더)
- **Path Variables**: `bandId` (UUID), `bandMemberId` (UUID)
- **Request Body** (optional)
  ```json
  {
    "role": "ADMIN"
  }
  ```
  - `role` enum — `LEADER` | `ADMIN` | `MEMBER`
- **비고**:
  - body 미제공 또는 `role=LEADER` 면 **리더 권한 위임** 동작 (현재 리더가 지정 멤버에게 권한 양도)
  - `role=ADMIN` / `role=MEMBER` 면 해당 역할로 변경

---

### 3-11. 밴드 탈퇴
- **DELETE** `/api/v1/bands/{bandId}/members/me`
- **인증 필요**
- **Path Variable**: `bandId` (UUID)

---

### 3-12. 밴드 정보 수정
- **PATCH** `/api/v1/bands/{bandId}`
- **인증 필요** (리더)
- **Path Variable**: `bandId` (UUID)
- **Request Body** (모든 필드 optional, 전달된 필드만 갱신)
  ```json
  {
    "name": "TuNA",
    "description": "성균관대학교 락밴드입니다.",
    "profileImg": "https://cdn/...jpg"
  }
  ```
- **Response**: `BandResponse`
  ```json
  {
    "bandId": "550e8400-e29b-41d4-a716-446655440000",
    "bandName": "TuNA"
  }
  ```

---

### 3-13. 밴드 삭제
- **DELETE** `/api/v1/bands/{bandId}`
- **인증 필요** (리더)
- **Path Variable**: `bandId` (UUID)
- **비고**: 밴드 및 소속 멤버를 cascade soft-delete.

---

### 3-14. 밴드 멤버 강퇴
- **DELETE** `/api/v1/bands/{bandId}/members/{bandMemberId}`
- **인증 필요** (리더)
- **Path Variables**: `bandId` (UUID), `bandMemberId` (UUID)
- **비고**: 리더 자신은 강퇴 불가.

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
  - `song`: **UUID 만 허용** (PracticeSong ID). 검색 결과의 텍스트/임시 식별자는 받지 않음.
  - `startAt` 형식: `yyyy-MM-dd HH:mm` (Asia/Seoul 기준). **현재 이후만 허용** (`@Future`); 과거 시각은 400 + `fieldErrors.startAt`.
- **Response**
  ```json
  {
    "practiceId": "550e8400-e29b-41d4-a716-446655440000",
    "practiceTitle": "TuNA 정기공연 1주차 합주"
  }
  ```
- **합주 시작하기 마법사 호출 시퀀스 (FE-API-020)**: PracticeSong 을 먼저 생성한 후 응답의 `songId` 를 `song` 필드로 전달. Practice 와 PracticeSong 의 1:1 바인딩은 PracticeSong 생성 시점에 `practiceId` 를 함께 전달하거나 (선바인딩), 본 API 의 `song` 으로 후바인딩.
  1. 외부 검색 결과 사용: `POST /practice-songs/from-song` (practiceId 생략) → `songId` 획득 → `POST /practices` 의 `song` 으로 전달
  2. 자작곡 입력: `POST /practice-songs` (practiceId 생략) → `songId` 획득 → `POST /practices` 의 `song` 으로 전달

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
  - `startAt` 형식: `yyyy-MM-dd HH:mm` (Asia/Seoul 기준). **현재 이후만 허용** (`@Future`).

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
  - `practiceId`: **optional**. 미제공 시 PracticeSong 만 생성하고 Practice 바인딩은 수행하지 않음 (마법사 시나리오에서 응답 `songId` 를 `POST /practices` 의 `song` 으로 전달).
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
  - `practiceId`: **optional**. 미제공 시 PracticeSong 만 생성 (5-2와 동일 마법사 시나리오 지원).
- **Response**: `PracticeSongResponse` (5-2와 동일 구조)
- **비고**: 외부 시스템에서 받아온 Song 객체를 그대로 사용하여 합주곡을 생성. `practiceId` 가 있으면 해당 합주에 1:1 바인딩, 없으면 응답 `songId` 를 [4-1](#4-1-합주-생성) 의 `song` 으로 전달하여 후바인딩. `song` 필드는 [5-1](#5-1-합주곡-검색-song) 응답의 항목을 재가공 없이 그대로 사용 가능.

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
  - `bandIds`: optional (미전달/`null`/빈 배열 모두 허용 — 빈 배열로 보정)
  - `venue`: optional
  - `startAt` 형식: `yyyy-MM-dd HH:mm` (Asia/Seoul 기준). **현재 이후만 허용** (`@Future`); 과거 시각은 400 + `fieldErrors.startAt`.
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
  - `startAt`: 전달 시 **현재 이후만 허용** (`@Future`).

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
  - `startAt`: **현재 이후만 허용** (`@Future`).
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

---

### 6-9. 공연 참여 밴드 일괄 추가
- **POST** `/api/v1/performances/{performanceId}/bands/batch`
- **인증 필요** (PerformanceManager)
- **Path Variable**: `performanceId` (UUID)
- **Request Body**
  ```json
  {
    "bandIds": [
      "550e8400-e29b-41d4-a716-446655440000",
      "550e8400-e29b-41d4-a716-446655440001"
    ]
  }
  ```
  - `bandIds`: not empty
- **Response**: `List<PerformanceBandResponse>`
  ```json
  [
    {
      "performanceBandId": "550e8400-e29b-41d4-a716-446655440010",
      "bandId": "550e8400-e29b-41d4-a716-446655440000"
    }
  ]
  ```
- **비고**: append 시맨틱. 이미 등록된 밴드는 무시하고 응답에서도 제외.

---

### 6-10. 공연 참여 밴드 단건 제거
- **DELETE** `/api/v1/performances/{performanceId}/bands/{bandId}`
- **인증 필요** (PerformanceManager)
- **Path Variables**: `performanceId` (UUID), `bandId` (UUID)
- **비고**: 공연에서 특정 참여 밴드 매핑을 제거 (밴드 자체는 삭제되지 않음).

---

## 7. 선곡 회의 (Setlist Meeting)

> Base path: `/api/v1/setlist-meetings`. 모든 엔드포인트 인증 필요.
>
> 권한 모델
> - **참여 멤버**: `SetlistMeetingMember` 에 등록된 사용자 또는 매니저
> - **매니저**: `SetlistMeeting.managerId` 와 일치하는 사용자
> - **잠금 상태(`lockedAt != null`) 제약**: 곡 생성/수정/삭제는 차단 (`SETLIST_MEETING_LOCKED`).
>
> 공통 응답 스키마
>
> `SetlistMeetingResponse`
> ```json
> {
>   "meetingId": "uuid",
>   "bandId": "uuid",
>   "title": "여름 페스티벌 셋리스트 회의",
>   "purpose": "PERFORMANCE",         // PERFORMANCE | GENERAL
>   "performanceId": "uuid|null",
>   "managerId": 1,
>   "lockedAt": "2026-04-26T12:30:45|null",
>   "createdAt": "...",
>   "updatedAt": "..."
> }
> ```
>
> `SetlistItemResponse`
> ```json
> {
>   "setlistItemId": "uuid",
>   "meetingId": "uuid",
>   "title": "Vicarious",
>   "artist": "Tool",
>   "album": "10,000 Days",
>   "duration": "07:06",
>   "proposerId": 1,
>   "note": "폴리리듬 도입부…",
>   "practiceSongId": "uuid|null",     // 매니저 잠금 시 매핑되는 합주곡 ID (cross-domain TODO)
>   "sessions": [
>     {
>       "sessionId": "G",
>       "label": "기타",
>       "short": "G",
>       "need": 1,
>       "custom": false,
>       "applicants": [2, 3],
>       "confirmed": [2]
>     }
>   ],
>   "createdAt": "...",
>   "updatedAt": "..."
> }
> ```

---

### 7-1. 선곡 회의 생성
- **POST** `/api/v1/setlist-meetings`
- **인증 필요**
- **Request Body**
  ```json
  {
    "title": "여름 페스티벌 셋리스트 회의",
    "purpose": "PERFORMANCE",
    "performanceId": "550e8400-e29b-41d4-a716-446655440000",
    "bandId": "550e8400-e29b-41d4-a716-446655440001",
    "managerId": 1,
    "participantUserIds": [1, 2, 3]
  }
  ```
  - `title`, `purpose`, `bandId`, `managerId` 필수
  - `purpose=PERFORMANCE` 일 때 `performanceId` 필수
  - `managerId` 는 `participantUserIds` 또는 호출자 본인에 포함되어야 함 (자동 추가됨)
- **Response**: `SetlistMeetingResponse`
- **검증**
  - `purpose=PERFORMANCE` + 동일 `performanceId` 활성 회의(`lockedAt=null`) 존재 → 409 `SETLIST_PERFORMANCE_HAS_ACTIVE_MEETING`
  - 매니저 미참여 → 400 `SETLIST_MANAGER_NOT_PARTICIPANT`
  - PERFORMANCE 인데 `performanceId` 누락 → 400 `SETLIST_PERFORMANCE_REQUIRED`

---

### 7-2. 내 선곡 회의 목록 조회 (커서 페이징)
- **GET** `/api/v1/setlist-meetings/me?lastId=&pageSize=20`
- **인증 필요**
- **Query Parameters**
  - `lastId` (UUID, optional): 직전 페이지 마지막 회의 ID
  - `pageSize` (Int, default `20`, 1~100)
- **Response**: `CursorResponse<SetlistMeetingResponse, UUID>`
- **비고**: 본인이 참여(`SetlistMeetingMember`) 중인 회의만 조회.

---

### 7-3. 선곡 회의 단건 조회
- **GET** `/api/v1/setlist-meetings/{meetingId}`
- **인증 필요** (참여 멤버 또는 매니저)
- **Response**: `SetlistMeetingDetailResponse`
  ```json
  {
    "meetingId": "uuid",
    "bandId": "uuid",
    "title": "...",
    "purpose": "GENERAL",
    "performanceId": null,
    "managerId": 1,
    "participantUserIds": [1, 2, 3],
    "lockedAt": null,
    "createdAt": "...",
    "updatedAt": "..."
  }
  ```
- **에러**: 404 `SETLIST_MEETING_NOT_FOUND`, 403 `SETLIST_MEETING_FORBIDDEN`

---

### 7-4. 선곡 회의 수정
- **PATCH** `/api/v1/setlist-meetings/{meetingId}`
- **인증 필요** (Manager)
- **Request Body**
  ```json
  {
    "title": "...",
    "managerId": 2
  }
  ```
  - 모든 필드 nullable. `managerId` 변경 시 새 매니저는 참여자에 포함되어야 함.
- **Response**: `SetlistMeetingResponse`
- **에러**: 403 `SETLIST_MEETING_NOT_MANAGER`, 400 `SETLIST_MANAGER_NOT_PARTICIPANT`

---

### 7-5. 선곡 회의 삭제
- **DELETE** `/api/v1/setlist-meetings/{meetingId}`
- **인증 필요** (Manager)
- **Response**: 204 (`ApiResponse.success()`, data=null)
- **비고**: soft-delete (`deleted_at` 세팅).

---

### 7-6. 선곡 항목 목록 조회 (커서 페이징)
- **GET** `/api/v1/setlist-meetings/{meetingId}/items?lastId=&pageSize=50`
- **인증 필요** (참여 멤버)
- **Query Parameters**: `lastId` (UUID), `pageSize` (1~200, default 50)
- **Response**: `CursorResponse<SetlistItemResponse, UUID>` — 응답 항목에 sessions/applicants/confirmed 포함.

---

### 7-7. 선곡 항목 단건 조회
- **GET** `/api/v1/setlist-meetings/{meetingId}/items/{itemId}`
- **인증 필요** (참여 멤버)
- **Response**: `SetlistItemResponse`

---

### 7-8. 선곡 항목 생성
- **POST** `/api/v1/setlist-meetings/{meetingId}/items`
- **인증 필요** (참여 멤버, 회의 진행 중)
- **Request Body**
  ```json
  {
    "title": "Vicarious",
    "artist": "Tool",
    "album": "10,000 Days",
    "duration": "07:06",
    "note": "폴리리듬 도입부…",
    "sessions": [
      { "sessionId": "V", "label": "보컬", "short": "V", "need": 1, "custom": false },
      { "sessionId": "G", "label": "기타", "short": "G", "need": 2, "custom": false }
    ]
  }
  ```
- **Response**: 생성된 `SetlistItemResponse` (applicants/confirmed 빈 버킷)
- **에러**: 409 `SETLIST_MEETING_LOCKED` (잠금 상태에서는 생성 불가)
- **비고**: `proposerId` 는 토큰의 사용자 ID로 자동 매핑.

---

### 7-9. 선곡 항목 부분 수정
- **PATCH** `/api/v1/setlist-meetings/{meetingId}/items/{itemId}`
- **인증 필요** (곡 제안자 또는 매니저)
- **Request Body**
  ```json
  {
    "title": "...",
    "artist": "...",
    "album": "...",
    "duration": "...",
    "note": "...",
    "sessions": [ /* 제공 시 새 세션 list 그대로 교체 */ ]
  }
  ```
- **Response**: 갱신된 `SetlistItemResponse`
- **비고**: `sessions` 변경 시 제거된 세션의 applicants/confirmed 는 cascade 정리.
- **에러**: 409 `SETLIST_MEETING_LOCKED`, 403 `SETLIST_ITEM_FORBIDDEN`

---

### 7-10. 선곡 항목 삭제
- **DELETE** `/api/v1/setlist-meetings/{meetingId}/items/{itemId}`
- **인증 필요** (곡 제안자 또는 매니저)
- **에러**: 409 `SETLIST_MEETING_LOCKED`, 403 `SETLIST_ITEM_FORBIDDEN`

---

### 7-11. 세션 지원
- **POST** `/api/v1/setlist-meetings/{meetingId}/items/{itemId}/sessions/{sessionId}/applicants`
- **인증 필요** (참여 멤버 본인)
- **비고**: 본인을 해당 세션 지원자로 등록. 중복 지원은 멱등 200.
- **에러**: 404 `SETLIST_ITEM_SESSION_NOT_FOUND`

---

### 7-12. 세션 지원 철회
- **DELETE** `/api/v1/setlist-meetings/{meetingId}/items/{itemId}/sessions/{sessionId}/applicants/{userId}`
- **인증 필요** (본인 — `userId` 가 토큰 사용자와 일치해야 함)
- **비고**: cascade — 본인이 해당 세션 confirmed 에 있으면 함께 제거.
- **에러**: 403 `SETLIST_ITEM_FORBIDDEN` (본인 외 철회 시도)

---

### 7-13. 세션 확정 / 해제 (매니저)
- **PATCH** `/api/v1/setlist-meetings/{meetingId}/items/{itemId}/sessions/{sessionId}/confirmations`
- **인증 필요** (Manager)
- **Request Body**
  ```json
  {
    "confirm":   [2, 3],
    "unconfirm": [4]
  }
  ```
- **Response**: 갱신된 `SetlistItemResponse`
- **에러**: 400 `SETLIST_ITEM_SESSION_FULL` (정원 `need` 초과), 404 `SETLIST_ITEM_SESSION_NOT_FOUND`, 403 `SETLIST_MEETING_NOT_MANAGER`

---

### 7-14. 곡별 채팅 조회 (커서 페이징)
- **GET** `/api/v1/setlist-meetings/{meetingId}/items/{itemId}/chat?lastId=&pageSize=50`
- **인증 필요** (참여 멤버)
- **Query Parameters**: `lastId` (UUID), `pageSize` (1~200, default 50)
- **Response**: `CursorResponse<SetlistChatMessageResponse, UUID>` — 최신순.
  ```json
  {
    "content": [
      {
        "messageId": "uuid",
        "setlistItemId": "uuid",
        "memberId": 1,
        "message": "보컬 음역대 빡셈",
        "createdAt": "..."
      }
    ],
    "nextCursor": "uuid|null",
    "hasNext": true
  }
  ```

---

### 7-15. 곡별 채팅 작성
- **POST** `/api/v1/setlist-meetings/{meetingId}/items/{itemId}/chat`
- **인증 필요** (참여 멤버)
- **Request Body**
  ```json
  { "message": "보컬 음역대 빡셈" }
  ```
  - `message`: 1~500자
- **Response**: 생성된 `SetlistChatMessageResponse`

---

### 7-16. 선곡 회의 잠금
- **POST** `/api/v1/setlist-meetings/{meetingId}/lock`
- **인증 필요** (Manager)
- **Response**: `SetlistLockResponse`
  ```json
  {
    "lockedAt": "2026-04-26T12:30:45",
    "songs": [
      { "setlistItemId": "uuid", "practiceSongId": "uuid|null" }
    ]
  }
  ```
- **에러**: 409 `SETLIST_MEETING_LOCKED` (이미 잠금 상태), 403 `SETLIST_MEETING_NOT_MANAGER`
- **비고**: 합주곡 벌크 생성 + Performance.setlist 자동 등록은 cross-domain 후속(TODO) — 현재는 잠금 상태 전이만 수행하며 `practiceSongId` 는 매핑 전이면 null.

---

### 7-17. 선곡 회의 잠금 해제
- **POST** `/api/v1/setlist-meetings/{meetingId}/unlock`
- **인증 필요** (Manager)
- **Response**: `SetlistMeetingResponse` (`lockedAt=null`)
- **에러**: 409 `SETLIST_MEETING_NOT_LOCKED`, 403 `SETLIST_MEETING_NOT_MANAGER`

# API 기능 명세서

Base URL: `/api/v1`

모든 응답은 `ApiResponse<T>` 래퍼로 감싸져 반환됩니다.  
인증이 필요한 API는 `Authorization: Bearer {accessToken}` 헤더를 사용합니다.

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
- **인증 불필요**
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
- **인증 불필요**
- **Query Parameters**
  | 파라미터 | 타입 | 필수 | 기본값 | 설명 |
  |---------|------|------|--------|------|
  | `lastId` | UUID | N | - | 이전 페이지 마지막 밴드 ID |
  | `pageSize` | Int (1~100) | N | 10 | 페이지 크기 |
- **Response**: `CursorResponse<BandInfoResponse, UUID>`

---

### 3-4. 밴드 멤버 단건 조회
- **GET** `/api/v1/bands/{bandId}/members/{bandMemberId}`
- **인증 불필요**
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
- **인증 불필요**
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
- **인증 불필요** (TODO: 인증 추가 예정)
- **Request Body**
  ```json
  {
    "title": "TuNA 정기공연 1주차 합주",
    "song": "550e8400-e29b-41d4-a716-446655440000",
    "venue": "TuNA",
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

## 미구현 (TODO)

`PracticeController`에 명시된 미구현 API 목록:

| 기능 | 설명 |
|------|------|
| 합주 조회 | 합주 단건/목록 조회 |
| 합주 세션 생성 | 합주 내 세션 추가 |
| 합주 세션 삭제 | 합주 세션 제거 |
| 합주 멤버 추가 | 합주에 멤버 추가 |
| 합주 세션 멤버 지정 | 세션 참여 신청 (본인) |
| 합주 세션 멤버 지정 취소 | 세션 참여 취소 (본인) |
| 합주 일정 변경 | 날짜/시간 수정 |
| 합주 장소 변경 | 장소 수정 |
| 합주 삭제 | 합주 soft delete |

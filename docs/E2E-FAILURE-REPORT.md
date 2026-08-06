# Bandage E2E 장애지점 보고서

| 항목 | 값 |
|---|---|
| 대상 | `https://bandage.team` 개발 서버 |
| 점검일 | 2026-08-06 |
| 방식 | API 직접 호출 (전 엔드포인트 + 엣지케이스) |
| 시드 API 호출 | 10169건 |
| 검증 케이스 | 138건 (PASS 129 / FAIL 9) |
| 확인된 결함 | **4건** (FAIL 9건이 4개 근본 원인으로 수렴) |

## 요약

| ID | 제목 | 심각도 | 영향 | 근본 원인 | 관련 케이스 |
|---|---|---|---|---|---|
| **D-01** | 회원가입/로그인 요청 검증 미동작 | **High** | 잘못된 형식의 이메일로 계정 생성 가능 | `@Valid` 누락 | A-03, A-03b, A-03c |
| **D-02** | 경로변수·쿼리 타입 불일치 시 500 | **Medium** | 클라이언트 오류가 서버 오류로 보고됨 | `MethodArgumentTypeMismatchException` 핸들러 부재 | C-04, H-05, L-05, M-01d |
| **D-03** | refresh 쿠키 누락 시 500 | **Medium** | 토큰 만료 재발급 실패가 500으로 표면화 | `MissingRequestCookieException` 핸들러 부재 | A-10 |
| **D-04** | 존재하지 않는 imageKey 포스터 등록 성공 | **Low** | 깨진 이미지 참조 데이터 생성 | S3 객체 존재 미검증 | J-08 |

---

## D-01. 회원가입/로그인 요청 검증 미동작 (High)

### 증상

DTO에 `@Email`, `@NotBlank`가 선언되어 있으나 **전혀 적용되지 않아** 잘못된 이메일로 가입이 성공합니다.

| 입력 이메일 | 기대 | 실제 | 결과 |
|---|---|---|---|
| `bad-format-a1b2c3` (@ 없음) | 400 | **200 생성됨** | FAIL |
| `@abc123.com` (로컬파트 없음) | 400 | **200 생성됨** | FAIL |
| `a b1234@c.com` (공백 포함) | 400 | **200 생성됨** | FAIL |
| `a@` (도메인 없음) | 400 | **200 생성됨** | FAIL |

### 원인

컨트롤러 파라미터에 `@Valid`가 없어 Bean Validation이 트리거되지 않습니다. DTO의 제약 애너테이션은 선언만 되어 있는 상태입니다.

```kotlin
// src/main/kotlin/.../domain/member/controller/MemberController.kt:45-46
fun joinMember(
    @RequestBody request: MemberJoinRequest,   // @Valid 없음
): ApiResponse<MemberResponse> = ...

// MemberJoinRequest — 제약은 선언되어 있으나 동작하지 않음
@NotBlank
@Email
val email: String,
```

### 영향 범위

프로젝트 전체 `@RequestBody` 45곳 중 **3곳만 `@Valid`가 누락**되어 있으며, 공교롭게 인증 관련 진입점입니다.

| 파일 | 라인 | 대상 DTO | 영향 |
|---|---|---|---|
| `domain/member/controller/MemberController.kt` | 46 | `MemberJoinRequest` | 이메일 형식·필수값 검증 무력화 |
| `domain/auth/controller/MemberAuthController.kt` | 33 | `MemberLoginRequest` | 로그인 입력 검증 무력화 |
| `domain/band/controller/BandController.kt` | 103 | `BandMemberRoleUpdateRequest?` | `required=false` 의도적 설계로 보이나 검증 부재 |

### 재현

```bash
curl -X POST https://bandage.team/api/v1/members/join \
  -H 'Content-Type: application/json' \
  -d '{"email":"not-an-email","name":"테스트","password":"pw1234"}'
# 기대: 400 INVALID_INPUT_VALUE
# 실제: 200 {"success":true,"data":{"id":...}}
```

### 권장 조치

`MemberController.kt:46`, `MemberAuthController.kt:33`에 `@Valid`를 추가합니다.

```kotlin
fun joinMember(
    @Valid @RequestBody request: MemberJoinRequest,
)
```

> 이미 형식이 잘못된 계정이 생성되어 있을 수 있으므로, 수정 후 기존 데이터 정합성 점검을 함께 권장합니다.

---

## D-02. 경로변수·쿼리 파라미터 타입 불일치 시 500 (Medium)

### 증상

UUID/날짜 타입 파라미터에 파싱 불가능한 값이 오면 **400이 아닌 500**을 반환합니다. 클라이언트 입력 오류가 서버 내부 오류로 잘못 보고됩니다.

| 케이스 | 요청 | 기대 | 실제 |
|---|---|---|---|
| C-04 | `GET /bands/not-a-uuid` | 400 | **500** |
| H-05 | `GET /me/availability/period?from=2026/08/10` | 400 | **500** |
| L-05 | `PATCH /notifications/99999999/read` (UUID 자리에 숫자) | 400 | **500** |
| M-01d | `PATCH /bands/{id}/members/{숫자}/role` | 400 | **500** |

### 원인

`GlobalExceptionHandler`에 `MethodArgumentTypeMismatchException` 핸들러가 없어, Spring의 타입 변환 실패가 catch-all `@ExceptionHandler(Exception::class)`로 떨어집니다.

```
src/main/kotlin/.../global/error/handler/GlobalExceptionHandler.kt
  21  @ExceptionHandler                              BusinessException
  29  @ExceptionHandler(MethodArgumentNotValidException)
  44  @ExceptionHandler(HttpMessageNotReadableException)
  76  @ExceptionHandler(HttpRequestMethodNotSupportedException)
  89  @ExceptionHandler(NoResourceFoundException)
  97  @ExceptionHandler(Exception)                   <- 타입 불일치가 여기로 → 500
      (MethodArgumentTypeMismatchException 핸들러 없음)
```

### 부수 효과

- 클라이언트가 재시도 가능한 오류(400)와 서버 장애(500)를 구분할 수 없습니다.
- 모니터링에서 정상적인 잘못된 요청이 서버 오류 알람으로 집계됩니다.
- 비즈니스 로직 자체는 정상입니다. 올바른 UUID를 쓰면 `404 NOTIFICATION_NOT_FOUND`, `403 NOTIFICATION_FORBIDDEN` 등이 정확히 반환됨을 확인했습니다(L-04, L-06, L-07 PASS).

### 권장 조치

```kotlin
@ExceptionHandler(MethodArgumentTypeMismatchException::class)
protected fun handleTypeMismatch(e: MethodArgumentTypeMismatchException) =
    ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE))
```

---

## D-03. refresh 토큰 쿠키 누락 시 500 (Medium)

### 증상

`POST /api/v1/auth/refresh` 호출 시 `refreshToken` 쿠키가 없으면 500을 반환합니다. 인증 실패(401) 또는 잘못된 요청(400)이어야 합니다.

| 요청 | 기대 | 실제 |
|---|---|---|
| `POST /auth/refresh` (쿠키 없음, 빈 바디) | 400 / 401 | **500 INTERNAL_SERVER_ERROR** |

### 원인

```kotlin
// src/main/kotlin/.../domain/auth/controller/MemberAuthController.kt:56-57
fun tokenRefresh(
    @CookieValue refreshToken: String,   // required=true, 쿠키 없으면 MissingRequestCookieException
    response: HttpServletResponse,
)
```
`MissingRequestCookieException` 역시 전용 핸들러가 없어 D-02와 동일하게 catch-all로 떨어집니다.

### 영향

access token 만료 후 재발급을 시도하는 클라이언트가 쿠키를 유실한 상황(브라우저 쿠키 차단, 앱 재설치 등)에서 500을 받게 되어, 재로그인 유도 분기를 태우기 어렵습니다.

### 권장 조치

`MissingRequestCookieException`(또는 상위 `ServletRequestBindingException`) 핸들러를 추가하고 401을 반환합니다.

---

## D-04. 존재하지 않는 imageKey로 포스터 등록 성공 (Low)

### 증상

S3에 실제로 존재하지 않는 `imageKey`로도 포스터 등록이 **200으로 성공**합니다.

| 요청 | 기대 | 실제 |
|---|---|---|
| `POST /performance-posters` `{imageKey: "poster/does/not/exist.png"}` | 400 / 404 | **200 등록됨** |

### 영향

- 클라이언트가 presigned URL 업로드에 실패한 뒤 등록만 호출하면 **깨진 이미지 참조 레코드**가 남습니다.
- 목록 조회 시 로딩 실패하는 포스터가 섞이며, 원인 추적이 어렵습니다.

### 권장 조치

등록 시 S3 `headObject`로 객체 존재를 확인하거나, presigned URL 발급 시 채번한 key를 서버가 보관해 대조합니다. 다만 매 등록마다 S3 호출이 추가되므로 트래픽 대비 비용 판단이 필요합니다.

> 판단 보류 항목: 업로드 완료 콜백 구조를 별도로 두는 설계도 가능합니다. 현재 구현 의도를 알 수 없어 확정 권고는 하지 않습니다.

---

## 정상 동작 확인 (오탐 아님을 검증한 항목)

점검 중 이상해 보였으나 **정상 동작으로 확인**된 항목입니다. 재점검 시 중복 조사를 피하기 위해 남깁니다.

| 현상 | 초기 판단 | 최종 결론 |
|---|---|---|
| 포스터 presign `413 FILE_SIZE_EXCEEDED` | 장애 의심 | **정상** — `poster4.jpg`(7.0MB), `poster6.png`(16.2MB)가 스펙상 5MB 한도 초과. 리사이즈 후 정상 등록 |
| 공연 생성 시 `403 SETLIST_FORBIDDEN` | 장애 의심 | **정상** — 타 밴드 셋리스트는 초대 수락 후 상대가 `setlists/batch`로 추가하는 설계 |
| 위임 API `400 INVALID_INPUT_VALUE` | 장애 의심 | **정상** — 요청 필드명 오류(`managerId`/`targetMemberId`가 정확). 올바른 필드로 403/200 정상 |
| 알림 읽음 처리 500 | 장애 의심 | **정상** — 응답 DTO 필드는 `id`. 올바른 UUID 사용 시 200/403/404 정확 |
| 곡 선택 `SETLIST_SELECTION_INCOMPLETE_SESSION` | 장애 의심 | **정상** — 모든 세션 확정이 선행되어야 하는 의도된 제약 |
| `GET /bands` pageSize 없이 200 | 스펙 불일치 의심 | **경미** — OpenAPI에 `pageSize` required이나 서버는 기본값 사용. 아래 참조 |

---

## 참고: OpenAPI 스펙과 실제 동작 차이 (결함 아님)

| 항목 | 스펙 | 실제 | 비고 |
|---|---|---|---|
| `GET /bands` 등 커서 페이징 | `query` 객체 required | 미전달 시에도 200 (기본값 적용) | 스펙상 required 표기가 실제와 다름 |
| 페이징 파라미터 직렬화 | `query` 객체 | 실제로는 `pageSize`, `lastId` 평면 파라미터 | 클라이언트 구현 시 혼동 소지 |
| presign 크기 초과 | 5MB 한도 명시 | `413` 반환 | 일반적으로 `400`을 쓰나 `413`도 의미상 타당 |

문서화 관점의 개선 여지이며 기능 결함은 아닙니다.

---

## 시드 중 발생한 실패 (9건, 전부 원인 규명 완료)

| 발생 지점 | 건수 | 원인 | 조치 |
|---|---|---|---|
| 공연 포스터 presign 413 | 5건 | 5MB 초과 이미지 | 리사이즈 후 재등록 완료 (18/18) |
| 밴드 프로필 이미지 presign 413 | 2건 | 동일 (밴드 4·6) | 동일 |
| 셋리스트/공연 위임 400 | 2건 | 스크립트 필드명 오류 | 올바른 필드로 재실행 성공 |

**시드 API 호출 10169건 중 실패 9건(0.09%)** 이며, 서버 결함으로 인한 실패는 없었습니다.

## 조치 우선순위

| 순위 | 결함 | 사유 |
|---|---|---|
| 1 | **D-01** `@Valid` 누락 | 데이터 정합성에 직접 영향. 수정 3줄로 해결 가능 |
| 2 | **D-02** 타입 불일치 500 | 4개 케이스에 광범위 영향. 핸들러 1개 추가로 해결 |
| 3 | **D-03** refresh 500 | 인증 흐름 예외 처리. D-02와 함께 처리 권장 |
| 4 | **D-04** imageKey 미검증 | 설계 판단 필요. 즉시성 낮음 |

D-01~D-03은 모두 소규모 수정으로 해결 가능하며, 세 건 모두 예외/검증 계층에 집중되어 있습니다.

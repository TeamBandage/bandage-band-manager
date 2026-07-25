# 선곡 항목 목록 필터 명세 (BD-228)

- 대상 API: `GET /api/v1/track-selections/{selectionId}/items` (`operationId: getItems`)
- 작성일: 2026-07-25

선곡 항목 목록에 4종의 독립 필터(f1 모집 상태 / f2 나의 지원 / f3 참여자 이름 / f4 트랙 정보)를 추가했다.
필터는 모두 커서 쿼리의 `WHERE` 로 내려가므로 기존 커서 페이징 동작이 그대로 유지된다.

---

## 1. 파라미터

| 파라미터 | 타입 | 필수 | 기본값 | 의미 |
|---|---|---|---|---|
| `lastId` | UUID | N | – | 커서. 이 ID **초과**(id 오름차순)부터 조회 |
| `pageSize` | Int (1..200) | N | 50 | 페이지 크기 |
| `status` | `List<RecruitStatus>` | N | 전체 | f1. 모집 상태. 목록 내부는 **OR** |
| `appliedByMe` | Boolean | N | 전체 | f2. `true`=내가 지원한 것만 / `false`=지원하지 않은 것만 |
| `memberName` | String (≤50) | N | – | f3. 지원자 이름 부분 검색(대소문자 무시) |
| `keyword` | String (≤100) | N | – | f4. 트랙 정보 부분 검색(대소문자 무시) |
| `searchFields` | `List<TrackSearchField>` | N | 3필드 전체 | f4의 검색 대상. 필드 간 **OR** |

`RecruitStatus` = `OPEN` \| `APPLY_COMPLETED` \| `ASSIGN_COMPLETED` \| `CLOSED`
`TrackSearchField` = `TITLE` \| `ARTIST` \| `ALBUM`

요청 예시:
```
GET /api/v1/track-selections/{id}/items?status=OPEN&status=CLOSED&appliedByMe=true&keyword=stairway&searchFields=TITLE&searchFields=ARTIST
```

---

## 2. 필터 결합 규칙

| 조합 | 결합 방식 |
|---|---|
| f1 × f2 × f3 × f4 | **AND** (서로 완전히 독립) |
| `status` 목록 내부 | OR (합집합) |
| `searchFields` 내부 | OR |
| `status` 미지정/빈 목록 | f1 미적용 |
| `appliedByMe` 미지정 | f2 미적용 |
| `memberName` 이 null·빈 문자열·공백뿐 | f3 미적용 |
| `keyword` 가 null·빈 문자열·공백뿐 | f4 미적용 (`searchFields` 를 보냈더라도 무시) |
| `keyword` 는 있고 `searchFields` 미지정 | 3필드 전체 OR 검색 |

`keyword` 없이 `searchFields` 만 오는 경우를 **에러가 아닌 no-op** 으로 정의한다.
검색창과 필드 체크박스가 독립적으로 동작하는 UI 를 400 으로 깨뜨리지 않기 위함이다.

---

## 3. RecruitStatus 정의

| 값 | 조건 | SQL 표현 |
|---|---|---|
| `OPEN` | 지원자가 없는 세션이 1개 이상, **또는 세션이 0개** | `EXISTS(session WHERE NOT EXISTS(applicant)) OR NOT EXISTS(session)` |
| `APPLY_COMPLETED` | 세션이 1개 이상 **AND** 모든 세션에 지원자 ≥ 1 | `EXISTS(session) AND NOT EXISTS(session WHERE NOT EXISTS(applicant))` |
| `ASSIGN_COMPLETED` | 세션이 1개 이상 **AND** 모든 세션에 확정자 ≥ 1 | `EXISTS(session) AND NOT EXISTS(session WHERE NOT EXISTS(confirmation))` |
| `CLOSED` | 매니저가 선곡 확정 | `is_selected = true` |

전칭(∀)은 이중 부정으로 표현한다: `∀session ∃applicant ≡ ¬∃session ¬∃applicant`.

소프트 삭제된 지원자/확정자는 `@SQLRestriction("deleted_at IS NULL")` 이 상관 서브쿼리에도 적용되어
자동으로 판정에서 제외된다(테스트로 검증됨).

---

## 4. 상태 겹침 (의도된 동작)

`status` 는 상호배타적 "진행 단계" 가 아니라 각각 독립적인 조건이다. 따라서 한 항목이 여러 상태를 동시에 만족한다.

| 항목 상태 | OPEN | APPLY_COMPLETED | ASSIGN_COMPLETED | CLOSED |
|---|:--:|:--:|:--:|:--:|
| 세션 0개 | ✅ | ❌ | ❌ | ❌ |
| 세션 2개, 지원자 0명 | ✅ | ❌ | ❌ | ❌ |
| 세션 2개, 1개만 지원자 있음 | ✅ | ❌ | ❌ | ❌ |
| 세션 2개 모두 지원자 있음, 확정 0 | ❌ | ✅ | ❌ | ❌ |
| 세션 2개 모두 확정 완료, 미선택 | ❌ | ✅ | ✅ | ❌ |
| 세션 2개 모두 확정 완료, 선택됨 | ❌ | ✅ | ✅ | ✅ |
| 확정 후 지원 철회로 확정도 삭제됨, 선택 상태 유지 | ✅ | ❌ | ❌ | ✅ |

마지막 행은 `withdrawSessionApplication` 이 확정까지 삭제하되 `isSelected` 는 유지하기 때문에 실제로 발생 가능한
조합이다. 즉 `CLOSED` 와 `OPEN` 이 동시에 참일 수 있어, 상태 공간은 전순서가 아니다.

---

## 5. 설계 결정

| 결정 | 채택 | 반대안 | 이유 |
|---|---|---|---|
| `status` 의미 | 겹치는 독립 술어(OR) | 상호배타 최고 단계 1개 | 리스트 파라미터 + 체크박스 UI 에 부합. `?status=APPLY_COMPLETED` 로 조회할 때 이미 확정된 항목(지원자 데이터가 가장 많은 항목)이 사라지는 것을 방지. 상태 공간이 실제로 전순서가 아님(§4 마지막 행) |
| 세션 0개 항목 | `OPEN` | `APPLY_COMPLETED`(공허참) | 모집을 시작하지도 않은 항목을 "완료" 로 분류하는 것은 무의미 |
| f3 "참여자" 정의 | 지원자(applicant)만 | 지원자 ∪ 확정자 | 정상 흐름상 확정자 ⊆ 지원자이므로 서브쿼리 1회를 절약. 검색 의도("이 사람이 지원한 곡")에도 부합. 단, `updateConfirmations` 는 지원 없이도 확정이 가능하므로 완전한 부분집합은 아니다 |
| 전칭(∀) 표현 | 이중 부정 `NOT EXISTS` | `count(distinct)` 비교 | 첫 반증 세션에서 단락 평가되어 빠름. 세션 교체 후 남은 고아 `session_id` 가 카운트를 부풀려 오탐하는 문제가 없음 |
| 필터 적용 위치 | SQL `WHERE` | 메모리 후필터 | 후필터는 `limit pageSize+1` 기반 `hasNext`/`nextCursor` 계산을 깨뜨림(§6) |
| 세션 컬렉션 참조 | 상관 서브쿼리 | outer join + `distinct` | outer join 은 세션 수만큼 행이 증식되어 `limit`/커서가 어긋남 |
| f3 회원 이름 조회 | `QMember` 조인 | `MemberService` 선조회 후 `in()` | `MemberRepository.findTop20By...` 는 20건에서 잘림, 중간 ID 목록도 무제한 |
| `keyword` 없는 `searchFields` | no-op | 400 에러 | 검색창과 필드 체크박스가 독립인 UI 를 깨지 않기 위함 |

---

## 6. 필터를 SQL 로 내려야 하는 이유

리포지토리는 `limit pageSize+1` 로 조회한 뒤 마지막 행을 버려 `hasNext` 를 판정하고, 반환된 마지막 행의 ID 를
`nextCursor` 로 삼는다. 필터를 조회 이후 메모리에서 적용하면:

- `hasNext` 가 "매칭되는 행이 더 있는가" 가 아니라 "필터 이전 행이 더 있는가" 를 뜻하게 되어 거짓이 된다.
- 페이지 크기가 예측 불가해진다(50건 요청 → 3건 반환 + `hasNext=true`).
- 한 페이지를 채우려면 매칭 50건이 모일 때까지 반복 조회해야 하고, 희귀 상태에서는 사실상 전체 스캔이 된다.
- `nextCursor` 를 반환된 마지막 행이 아니라 **스캔한** 마지막 행에서 뽑아야 하며, 이를 틀리면 행이 조용히
  누락되거나 중복된다.

모든 술어가 `EXISTS`/스칼라 조건이므로 outer `FROM` 은 단일 테이블로 유지된다.
즉 항목당 1행이 보장되어 `.distinct()` 없이도 커서 계산이 정확하다.

---

## 7. 성능 노트

- f1/f2 상관 서브쿼리는 기존 유니크 인덱스가 선두 컬럼으로 커버한다:
  `uk_track_selection_item_applicant (track_selection_item_id, session_id, member_id)`,
  `uk_track_selection_item_confirmation` 동일 구조.
- 응답 조립(지원자·확정자·회원요약 bulk 조회)은 기존과 동일하게 `result.content` 기준으로 1회씩 수행되어
  목록 단위 N+1 이 발생하지 않는다.
- f3/f4 는 선행 와일드카드(`%x%`) 검색이라 btree 인덱스를 쓸 수 없고 `p_member` / `p_track_selection_item`
  스캔이 발생한다. 현재 데이터 규모에서는 허용 가능하며, 필요해지면 `pg_trgm` GIN 인덱스가 후속 개선안이다.
- `p_track_selection_item_session(track_selection_item_id)` 인덱스 존재 여부는 미확인이다.
  `@CollectionTable` 에 명시적 인덱스가 없어 Hibernate DDL 이 생성했는지 확인이 필요하며,
  없다면 별도 changelog 로 추가를 검토한다.

---

## 8. 검증

`src/test/kotlin/com/bandage/bandmanager/domain/selection/repository/TrackSelectionItemFilterTest.kt`
(`@DataJpaTest`, H2 PostgreSQL 모드 — Docker 불필요)

```bash
./gradlew test --tests '*TrackSelectionItemFilterTest'
```

검증 항목: §4 진리표 전 행, 상태 목록 OR, `appliedByMe` 3분기, 이름 검색 대소문자 무시,
`searchFields` 지정/미지정, 앨범 null 미매치, 필터 4종 AND 조합,
소프트 삭제 지원자 제외, **필터 적용 후 커서 순회 무중복·무누락 및 최종 `hasNext=false`**.

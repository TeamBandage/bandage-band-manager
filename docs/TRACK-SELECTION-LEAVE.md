# 선곡 회의 떠나기 정책 (BD-218)

- 대상 API: `DELETE /api/v1/track-selections/{selectionId}/members/me` (`operationId: leaveSelection`)
- 작성일: 2026-07-25

기존에는 매니저가 참여자를 제거하는 경로(`PATCH /{selectionId}/participants`)만 있었고, 참여자가 스스로
회의를 떠날 방법이 없었다. 밴드 탈퇴(`DELETE /bands/{bandId}/members/me`)와 동일한 경로 규약으로 자율 이탈을 추가한다.

---

## 1. 처리 대상과 정책

떠나는 멤버를 `L`, 대상 회의를 `S` 라 할 때 다음 순서로 처리한다.

| 순서 | 대상 | 처리 | 비고 |
|---|---|---|---|
| 0 | 참여 자격 | `TrackSelectionMember` 가 없으면 **403** | `SETLIST_MEETING_FORBIDDEN` |
| 1 | `S.managerId == L` 인 경우 | 후임에게 매니저 권한 양도 (§2) | 후보 없으면 §3 으로 종료 |
| 2 | `TrackSelectionItemApplicant` (L의 세션 지원) | **전건 삭제** | `deleteAllByItemInAndMemberId` |
| 3 | `TrackSelectionItemConfirmation` (L의 세션 확정) | **전건 삭제** | 동일 |
| 4 | 2·3 으로 영향받은 아이템의 `isSelected` | **false 로 해제** | L 이 관여한 아이템만. 전체 일괄 해제 아님 |
| 5 | `proposerId == L` 인 아이템 | **`proposerId = null`** | 아이템 자체는 유지 |
| 6 | `TrackSelectionMember` (참여 연결) | **삭제** | |

`isSelected` 를 해제하는 이유: 선곡 확정은 "모든 세션의 확정 인원이 충족됨" 을 전제로 하는데
(`validateAllSessionsConfirmed`), L 의 지원·확정이 사라지면 그 전제가 깨진다. 확정 상태만 남겨두면
`CLOSED` 이면서 동시에 `OPEN` 인 모순된 항목이 된다.

아이템 자체는 삭제하지 않는다. 곡 제안은 회의의 공동 자산이며, 제안자가 떠났다는 이유로 다른 참여자의
지원·확정 이력까지 사라지는 것은 과도하다.

---

## 2. 매니저가 떠날 때의 후임 선정

회원 탈퇴 경로(`cleanupOnWithdrawal`)와 **동일한 티어 정책**을 공유한다(`selectSuccessor`).

| 티어 | 후보 | 선정 기준 | 추가 처리 |
|---|---|---|---|
| 1 | 회의 참여자(`TrackSelectionMember`) 중 떠나는 멤버 제외 | `createdAt` 최소(최고참) | 이미 참여자이므로 `changeManager` 만 |
| 2 | 연결된 밴드(`TrackSelectionBand`)의 일반 멤버 중 기존 참여자·떠나는 멤버 제외 | 밴드 등록 순 → 그 밴드 내 `createdAt` 최소 | 신규 매니저를 **참여자로도 등록** (매니저 ⊆ 참여자 불변식 유지) |
| 후보 전무 | – | – | **회의 소프트 삭제** (§3) |

---

## 3. 마지막 참여자가 떠나는 경우

티어1·티어2 모두 후보가 없다 = 회의에 남을 사람이 아무도 없다는 뜻이다.
이때는 `selection.markAsDeleted(L)` 로 회의를 소프트 삭제하고 **즉시 종료**한다
(참여자 연결 삭제나 아이템 정리를 별도로 수행하지 않는다 — 회의 자체가 조회 대상에서 사라지므로).

매니저 없는 회의를 남기지 않는 이유: 잠금(`lock`) 해제·항목 확정·참여자 변경이 모두 매니저 전용이라,
매니저가 없으면 어떤 방식으로도 회의를 진행하거나 해소할 수 없다.

---

## 4. `proposerId` nullable 전환에 따른 권한 조건 변경

`TrackSelectionItem.proposerId` 를 `Long` → `Long?` 으로 변경했다(DB 컬럼도 `NOT NULL` 해제).
이에 따라 항목 수정/삭제 권한 검사 순서를 조정했다.

| | 변경 전 | 변경 후 |
|---|---|---|
| 통과 조건 | 제안자 **OR** 매니저 | 매니저 **OR** 제안자 (동일) |
| 예외 조건(코드) | `item.proposerId != memberId && selection.managerId != memberId` | `selection.managerId != memberId && item.proposerId != memberId` |
| `proposerId == null` 일 때 | 제안자 비교를 먼저 수행 | **매니저 비교를 먼저 수행** |

`managerId` 는 non-null 이 보장되므로 매니저 비교를 앞에 두면 제안자 없는 항목에서도 의도가 코드 순서로
드러난다. 실질 동작은 다음과 같다.

| 항목 상태 | 매니저 | 제안자 본인 | 제3의 참여자 |
|---|:--:|:--:|:--:|
| `proposerId` 있음 | ✅ 수정/삭제 | ✅ 수정/삭제 | ❌ 403 |
| `proposerId == null`(제안자가 떠남) | ✅ 수정/삭제 | – | ❌ 403 |

적용 지점: `TrackSelectionService.updateItem`, `TrackSelectionService.deleteItem`.

---

## 5. 응답 영향

`TrackSelectionItemResponse.proposer` 는 이미 nullable 이며, 제안자가 회의를 떠난 경우에도 `null` 이 된다
(기존에는 탈퇴 회원인 경우에만 `null` 이었다). 클라이언트는 두 경우를 구분하지 않고 "제안자 정보 없음" 으로
표시하면 된다.

---

## 6. 관련 에러 코드

| 코드 | 상태 | 발생 조건 |
|---|---|---|
| `SETLIST_MEETING_NOT_FOUND` | 404 | 존재하지 않거나 이미 삭제된 회의 |
| `SETLIST_MEETING_FORBIDDEN` | 403 | 참여자가 아닌 멤버가 떠나기를 요청 |

잠금(`lockedAt`) 상태는 떠나기를 막지 않는다. 잠금은 항목 추가/수정을 제한하는 장치이지 참여자를 붙잡아
두는 장치가 아니며, 잠긴 회의에 원치 않는 참여자를 강제로 남겨둘 이유가 없다.

---

## 7. 검증

`src/test/kotlin/com/bandage/bandmanager/domain/selection/service/TrackSelectionServiceLeaveTest.kt`

```bash
./gradlew test --tests '*TrackSelectionServiceLeaveTest'
```

검증 항목: 참여 연결 삭제, 지원·확정 삭제 및 영향 아이템만 `isSelected` 해제, 확정만 있던 아이템도 해제,
제안자 `null` 전환(타인 제안 아이템은 유지), 매니저 떠날 때 후임 양도, 마지막 참여자면 회의 소프트 삭제,
비참여자 403.

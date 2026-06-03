# BD-33 (PRD-2) 합주 스케줄링 재설계 — 마이그레이션 계획

본 문서는 PRD-2 적용에 따른 DB 마이그레이션, Dual-Write, 백필, 롤백, 레거시 폐기 계획을 정리한다.
(전제: 현재 운영 데이터 없음 — 대부분의 백필은 no-op 에 가깝다. 운영 적용 시 본 절차를 따른다.)

## 1. Liquibase changelog 인벤토리

| id | 내용 | 적용 |
|----|------|------|
| 001 | 초기 스키마 | 적용 |
| 002 | setlist/selection 리팩토링 | 적용 |
| 003 | DEVELOPER 시드 계정 | 적용 |
| 004 | TrackInfo 도입 + PracticeSong 제거 (BD-70) | 적용 |
| 005 | participant session_id + setlist_id (BD-70) | 적용 |
| 006 | Practice → Jam 리네이밍 (BD-70) | 적용 |
| 008 | MemberAvailability 신설 (T2) | 적용 |
| 009 | JamReservation 신설 (T3) | 적용 |
| 010 | ScheduleBoard performance_id/window (T5) | 적용 |
| 011 | ScheduleBlockTrack N:M (T6) | 적용 |
| 012 | ScheduleBlock song_id 제거 + 반복/배치 메타 (T6/T7) | 적용 |
| 013 | ScheduleBlockJam 추적 (T8) | 적용 |
| (deferred) | `deferred/drop-legacy-meeting-scope.sql` — meeting_id 등 레거시 컬럼 제거 | **미적용(백필 검증 후 수동 승격)** |

> 007 은 BD-70 이 rename 을 006 으로 통합하여 공번이다(PRD 문서 번호 체계 정렬 목적).

## 2. Dual-Write (T14.2)

ScheduleBoard 는 PRD-2 에서 `performance_id`(필수) 로 스코프가 이전되었고, 구 `meeting_id` 는 **nullable** 로 유지된다.

- **신규 보드**: `performance_id` 만 기록(`meeting_id = null`). 별도 Dual-Write 불필요.
- **운영 전환 시(레거시 meeting 기반 보드가 있는 경우)**: 전환 기간 동안 보드 생성/수정 시 가능한 경우 두 값을 함께 기록(코드 레벨에서 meeting↔performance 매핑이 확보될 때). 본 PR 기준 운영 데이터가 없어 코드 경로는 performance 단일 기록이다.

## 3. 백필 (T14.3)

레거시 meeting 기반 보드가 존재할 경우:
1. meeting(TrackSelection) → performance 매핑 확보(셋리스트/공연 연결 기준).
2. `UPDATE p_schedule_board SET performance_id = :resolved WHERE performance_id IS NULL` 형태로 채움.
3. 매핑 불가 보드는 수기 검토 대상으로 리포트.

현재 운영 데이터 없음 → 백필 no-op. (스크립트는 운영 적용 시 별도 changeset 으로 추가)

## 4. 롤백 계획 (T14.4)

- 추가형 changelog(008~013)는 **테이블 신설/컬럼 추가** 위주로, 롤백 시 신규 테이블 drop + 추가 컬럼 drop 으로 복원 가능.
- 파괴적 변경(`p_schedule_block.song_id` 제거 = 012)은 운영 데이터 없음 전제에서 수행. 데이터 존재 시에는 012 적용 전 `song_id → p_schedule_block_track` 백필 후 컬럼 제거할 것.
- 레거시 컬럼 제거(meeting_id 등)는 본 PR 에서 **수행하지 않는다**. `deferred/drop-legacy-meeting-scope.sql` 로 분리하여 백필/검증 완료 후 master 에 등록한다.

## 5. MemberSchedule 폐기 (T15)

- **T15.1 전환**: `MemberScheduleMigrationService` 가 회의 단위 MemberSchedule 의 available/unavailable 날짜를 글로벌 `MemberAvailability` 의 전일 예외(AVAILABLE/BLOCKED)로 이관한다. 같은 날짜 충돌 시 BLOCKED 우선. 슬롯 비트맵(blocks)은 별도 정밀 이관 대상으로 본 전환에서 생략.
- **T15.2 호환성**: `MemberScheduleController` 는 `@Deprecated` 로 표기하고 당분간 동작을 유지한다(신규 연동 금지). 글로벌 가용성은 `GET/PUT /me/availability` 사용.
- **T15.3 가용성 출처**: 신 스케줄 보드는 Performance 스코프이므로, 가용성 판단은 글로벌 `MemberAvailability` 를 단일 출처로 사용한다. 회의 단위 MemberSchedule 로의 런타임 폴백은 스코프 불일치로 채택하지 않으며, 전환은 위 마이그레이션 서비스로 일괄 수행한다.
- **T15.4 최종 폐기**: 모든 멤버 전환 완료 + 신 API 안정화 확인 후, `MemberSchedule` 관련 테이블/엔티티/컨트롤러를 제거하는 changeset 을 등록한다(deferred).

## 6. 적용 순서 요약

1. 008~013 적용(본 PR 묶음, 운영 반영)
2. (운영 데이터 존재 시) song_id/meeting_id 백필 스크립트 추가·실행
3. `MemberScheduleMigrationService.migrateAll()` 1회 실행
4. 신 가용성/자동배치 API 안정화 모니터링
5. `deferred/drop-legacy-meeting-scope.sql` 를 master 에 등록하여 레거시 컬럼/테이블 제거

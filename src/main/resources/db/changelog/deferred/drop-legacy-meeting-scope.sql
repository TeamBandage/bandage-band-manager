-- BD-33 (PRD-2 T14.4 / T15.4): 레거시 meeting 스코프 및 MemberSchedule 제거 (지연 실행)
--
-- ⚠️ 이 스크립트는 db.changelog-master.yaml 에 등록되어 있지 않다(자동 적용 안 됨).
--    백필/전환 검증 완료 후, 정식 changeId(예: 014-drop-legacy-meeting-scope)로 master 에 승격하여 적용한다.
--    적용 전 반드시: (1) p_schedule_board.meeting_id 의존 코드 제거, (2) MemberScheduleMigrationService 전환 완료 확인.

-- 1) ScheduleBoard 의 구 meeting 스코프 컬럼 제거
ALTER TABLE public.p_schedule_board DROP COLUMN IF EXISTS meeting_id;

-- 2) MemberSchedule(회의 단위 가용성) 폐기 — MemberAvailability 로 전환 완료 후
DROP TABLE IF EXISTS public.p_member_schedule_blocks;
DROP TABLE IF EXISTS public.p_member_schedule_available_dates;
DROP TABLE IF EXISTS public.p_member_schedule_unavailable_dates;
DROP TABLE IF EXISTS public.p_member_schedule;

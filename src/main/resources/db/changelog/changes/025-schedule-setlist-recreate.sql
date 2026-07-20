-- BD-172: schedule 도메인을 셋리스트 스코프로 재구성
--         ScheduleBoard/ScheduleBlock 을 setlistId 스코프 + startDate/endDate/slot 구조로 재설계.
--         운영 데이터 없음 가정(스코프 전환 전 단계).

-- ===== p_schedule_board: performance/meeting 스코프 → setlist 스코프 =====
ALTER TABLE public.p_schedule_board ADD COLUMN setlist_id uuid;
ALTER TABLE public.p_schedule_board ALTER COLUMN setlist_id SET NOT NULL;

ALTER TABLE public.p_schedule_board DROP COLUMN performance_id;
ALTER TABLE public.p_schedule_board DROP COLUMN meeting_id;
ALTER TABLE public.p_schedule_board DROP COLUMN palette_seed;
ALTER TABLE public.p_schedule_board DROP COLUMN version;

DROP INDEX IF EXISTS public.idx_schedule_board_performance;
CREATE INDEX idx_schedule_board_setlist ON public.p_schedule_board (setlist_id);

-- ===== p_schedule_block: date+durationSlots → startDate/endDate + startSlot/endSlot, titleOverride → title =====
ALTER TABLE public.p_schedule_block ADD COLUMN start_date date;
ALTER TABLE public.p_schedule_block ADD COLUMN end_date date;
ALTER TABLE public.p_schedule_block ADD COLUMN end_slot integer;

-- 구 컬럼 값을 새 구조로 이전할 데이터가 없으므로 NOT NULL 직접 설정
ALTER TABLE public.p_schedule_block ALTER COLUMN start_date SET NOT NULL;
ALTER TABLE public.p_schedule_block ALTER COLUMN end_date SET NOT NULL;
ALTER TABLE public.p_schedule_block ALTER COLUMN end_slot SET NOT NULL;

ALTER TABLE public.p_schedule_block DROP COLUMN block_date;
ALTER TABLE public.p_schedule_block DROP COLUMN duration_slots;
ALTER TABLE public.p_schedule_block DROP COLUMN palette_index;

-- titleOverride → title 로 의미 일반화(블록 단위 라벨)
ALTER TABLE public.p_schedule_block RENAME COLUMN title_override TO title;

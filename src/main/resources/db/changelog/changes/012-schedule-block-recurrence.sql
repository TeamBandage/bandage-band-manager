-- BD-33 (PRD-2 Task 6/7): ScheduleBlock 단일 songId 제거 + 반복/배치 메타데이터 추가
-- 운영 데이터 없음 가정.

-- 6) 단일 song 참조 제거 (N:M p_schedule_block_track 로 대체)
ALTER TABLE public.p_schedule_block DROP COLUMN song_id;

-- songTitleOverride → titleOverride 로 의미 일반화(블록 단위 라벨 오버라이드)
ALTER TABLE public.p_schedule_block RENAME COLUMN song_title_override TO title_override;

-- 7) 반복 배치 규칙(RecurrenceRule)
ALTER TABLE public.p_schedule_block ADD COLUMN recurrence_freq character varying(20) NOT NULL DEFAULT 'ONCE';
ALTER TABLE public.p_schedule_block ALTER COLUMN recurrence_freq DROP DEFAULT;
ALTER TABLE public.p_schedule_block ADD COLUMN recurrence_interval integer NOT NULL DEFAULT 1;
ALTER TABLE public.p_schedule_block ALTER COLUMN recurrence_interval DROP DEFAULT;
ALTER TABLE public.p_schedule_block ADD COLUMN recurrence_until date;
ALTER TABLE public.p_schedule_block ADD COLUMN recurrence_count integer;

-- 배치 출처(PlacementOrigin)
ALTER TABLE public.p_schedule_block ADD COLUMN placement_origin character varying(20) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE public.p_schedule_block ALTER COLUMN placement_origin DROP DEFAULT;

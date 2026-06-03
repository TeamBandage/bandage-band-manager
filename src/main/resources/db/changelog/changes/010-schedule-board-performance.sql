-- BD-33 (PRD-2 Task 5): ScheduleBoard 스코프를 meeting → performance 로 이전
-- 운영 데이터 없음 가정.

ALTER TABLE public.p_schedule_board ADD COLUMN performance_id uuid;
ALTER TABLE public.p_schedule_board ALTER COLUMN performance_id SET NOT NULL;

-- 구 meeting 스코프: 마이그레이션/dual-write 기간 동안만 유지(nullable). 추후 drop.
ALTER TABLE public.p_schedule_board ALTER COLUMN meeting_id DROP NOT NULL;

-- 보드 레벨 연습 가능 날짜 범위
ALTER TABLE public.p_schedule_board ADD COLUMN window_from date;
ALTER TABLE public.p_schedule_board ADD COLUMN window_to date;

CREATE INDEX idx_schedule_board_performance ON public.p_schedule_board (performance_id);

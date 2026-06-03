-- BD-33 (PRD-2 Task 8): ScheduleBlock → 확정 생성 Jam 추적 매핑 테이블 신설
-- 파생 추적 테이블이므로 감사/소프트삭제 필드 없음.

CREATE TABLE public.p_schedule_block_jam (
    schedule_block_jam_id uuid NOT NULL,
    schedule_block_id uuid NOT NULL,
    schedule_board_id uuid NOT NULL,
    jam_id uuid NOT NULL,
    CONSTRAINT p_schedule_block_jam_pkey PRIMARY KEY (schedule_block_jam_id)
);

CREATE INDEX idx_schedule_block_jam_block ON public.p_schedule_block_jam (schedule_block_id);
CREATE INDEX idx_schedule_block_jam_board ON public.p_schedule_block_jam (schedule_board_id);

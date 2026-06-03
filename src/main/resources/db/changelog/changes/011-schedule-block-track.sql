-- BD-33 (PRD-2 Task 6): ScheduleBlock ↔ SetlistTrack N:M 매핑 테이블 신설

CREATE TABLE public.p_schedule_block_track (
    schedule_block_track_id uuid NOT NULL,
    schedule_block_id uuid NOT NULL,
    setlist_track_id uuid NOT NULL,
    ordinal integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by bigint,
    last_modified_at timestamp(6) without time zone NOT NULL,
    last_modified_by bigint,
    deleted_at timestamp(6) without time zone,
    deleted_by bigint,
    CONSTRAINT p_schedule_block_track_pkey PRIMARY KEY (schedule_block_track_id),
    CONSTRAINT uk_schedule_block_track UNIQUE (schedule_block_id, setlist_track_id),
    CONSTRAINT fk_schedule_block_track__block
        FOREIGN KEY (schedule_block_id) REFERENCES public.p_schedule_block(schedule_block_id) ON DELETE CASCADE
);

CREATE INDEX idx_schedule_block_track__block ON public.p_schedule_block_track (schedule_block_id);

-- BD-272: 스케줄보드 ↔ 셋리스트 트랙 단위 자동배치 결과 테이블 신설
--         트랙별 배치 회차 수를 기록한다. placement_count = 0 이면 미배치.
--         미배치 트랙은 블록이 없어 p_schedule_block_track 으로 표현할 수 없으므로 별도 테이블이 필요하다.

CREATE TABLE public.p_schedule_board_setlist_item_placement (
    schedule_board_setlist_item_placement_id uuid NOT NULL,
    schedule_board_id uuid NOT NULL,
    setlist_track_id uuid NOT NULL,
    placement_count integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by bigint,
    last_modified_at timestamp(6) without time zone NOT NULL,
    last_modified_by bigint,
    deleted_at timestamp(6) without time zone,
    deleted_by bigint,
    CONSTRAINT p_schedule_board_setlist_item_placement_pkey PRIMARY KEY (schedule_board_setlist_item_placement_id),
    CONSTRAINT uk_schedule_board_setlist_item_placement UNIQUE (schedule_board_id, setlist_track_id),
    CONSTRAINT fk_schedule_board_setlist_item_placement__board
        FOREIGN KEY (schedule_board_id) REFERENCES public.p_schedule_board(schedule_board_id) ON DELETE CASCADE
);

CREATE INDEX idx_schedule_board_setlist_item_placement__board
    ON public.p_schedule_board_setlist_item_placement (schedule_board_id);

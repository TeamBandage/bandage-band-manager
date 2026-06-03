-- BD-33 (PRD-2 Task 3): JamReservation 추적 테이블 신설
-- 확정 Jam 이 점유하는 멤버별 시간 구간. JamReservationSyncService 가 delete-reinsert 로 동기화.
-- 감사/소프트삭제 필드 없음(파생 테이블).

CREATE TABLE public.p_jam_reservation (
    jam_reservation_id uuid NOT NULL,
    member_id bigint NOT NULL,
    jam_id uuid NOT NULL,
    start_at timestamp(6) without time zone NOT NULL,
    end_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT p_jam_reservation_pkey PRIMARY KEY (jam_reservation_id)
);

-- 멤버별 시간 충돌 조회 성능 최적화
CREATE INDEX idx_jam_reservation_member_time
    ON public.p_jam_reservation (member_id, start_at, end_at);

-- jam 단위 조회/삭제(sync) 최적화
CREATE INDEX idx_jam_reservation_jam
    ON public.p_jam_reservation (jam_id);

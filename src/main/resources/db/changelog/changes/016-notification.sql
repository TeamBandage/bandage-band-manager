-- BD-81: 알림(Notification) 도메인 테이블 신설
-- category 는 enum 을 STRING(varchar)으로 저장한다. 알림 카테고리는 추가/삭제가 빈번하고
-- 프론트 필터 키로 쓰이므로, 순서 변경에 취약한 smallint(ORDINAL) 대신 varchar 를 사용한다.

CREATE TABLE public.p_notification (
    notification_id uuid NOT NULL,
    recipient_id bigint NOT NULL,
    category character varying(50) NOT NULL,
    title character varying(255) NOT NULL,
    message character varying(500) NOT NULL,
    reference_id character varying(255),
    is_read boolean NOT NULL DEFAULT false,
    read_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    created_by bigint,
    last_modified_at timestamp(6) without time zone NOT NULL,
    last_modified_by bigint,
    deleted_at timestamp(6) without time zone,
    deleted_by bigint,
    CONSTRAINT p_notification_pkey PRIMARY KEY (notification_id)
);

-- 수신자별 최신순 조회
CREATE INDEX idx_notification__recipient ON public.p_notification (recipient_id, created_at);

-- 스케줄러 멱등성 조회(recipient_id + category + reference_id)
CREATE INDEX idx_notification__idempotency ON public.p_notification (recipient_id, category, reference_id);

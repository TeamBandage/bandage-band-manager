-- BD-78: 공연 매니저 역할(OWNER/MANAGER) 컬럼 + 공연 초대 테이블 신설

-- 1) p_performance_manager 에 역할 컬럼 추가 (0=OWNER, 1=MANAGER)
ALTER TABLE public.p_performance_manager
    ADD COLUMN role smallint;

-- 기존 매니저 행은 모두 공연 생성자이므로 OWNER(0) 로 백필
UPDATE public.p_performance_manager
SET role = 0
WHERE role IS NULL;

ALTER TABLE public.p_performance_manager
    ALTER COLUMN role SET NOT NULL;

ALTER TABLE public.p_performance_manager
    ADD CONSTRAINT p_performance_manager_role_check CHECK (role IN (0, 1));

-- 동일 공연-멤버 매니저 중복 방지
ALTER TABLE public.p_performance_manager
    ADD CONSTRAINT uk_performance_manager UNIQUE (performance_id, member_id);

-- 2) 공연 초대 테이블 (0=PENDING, 1=ACCEPTED, 2=REJECTED, 3=CANCELED)
CREATE TABLE public.p_performance_invitation (
    performance_invitation_id uuid NOT NULL,
    performance_id uuid NOT NULL,
    invited_member_id bigint NOT NULL,
    invited_by bigint NOT NULL,
    status smallint NOT NULL,
    processed_by bigint,
    created_at timestamp(6) without time zone NOT NULL,
    created_by bigint,
    last_modified_at timestamp(6) without time zone NOT NULL,
    last_modified_by bigint,
    deleted_at timestamp(6) without time zone,
    deleted_by bigint,
    CONSTRAINT p_performance_invitation_pkey PRIMARY KEY (performance_invitation_id),
    CONSTRAINT p_performance_invitation_status_check CHECK (status >= 0 AND status <= 3),
    CONSTRAINT fk_performance_invitation__performance
        FOREIGN KEY (performance_id) REFERENCES public.p_performance(performance_id) ON DELETE CASCADE
);

CREATE INDEX idx_performance_invitation__performance ON public.p_performance_invitation (performance_id);

CREATE INDEX idx_performance_invitation__invited_member ON public.p_performance_invitation (invited_member_id);

-- 동일 공연에 대한 동일 멤버의 PENDING 초대 중복 방지 (취소/처리된 건 재초대 허용)
CREATE UNIQUE INDEX uk_performance_invitation_pending
    ON public.p_performance_invitation (performance_id, invited_member_id)
    WHERE status = 0 AND deleted_at IS NULL;

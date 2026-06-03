-- BD-33 (PRD-2 Task 2): MemberAvailability 도메인 테이블 신설
-- 멤버 글로벌 가용성: 주간 반복 규칙(WeeklyRule) + 날짜별 예외(AvailabilityException)
-- 운영 데이터 없음 가정.

-- 1) p_member_availability: member_id 를 PK 로 사용(auto-gen 아님)
CREATE TABLE public.p_member_availability (
    member_id bigint NOT NULL,
    note character varying(500),
    created_at timestamp(6) without time zone NOT NULL,
    created_by bigint,
    last_modified_at timestamp(6) without time zone NOT NULL,
    last_modified_by bigint,
    deleted_at timestamp(6) without time zone,
    deleted_by bigint,
    CONSTRAINT p_member_availability_pkey PRIMARY KEY (member_id),
    CONSTRAINT fk_member_availability__member
        FOREIGN KEY (member_id) REFERENCES public.p_member(member_id)
);

-- 2) p_member_availability_weekly_rules: 주간 반복 규칙 (ElementCollection, bag)
CREATE TABLE public.p_member_availability_weekly_rules (
    member_id bigint NOT NULL,
    day_of_week character varying(10) NOT NULL,
    start_slot integer NOT NULL,
    end_slot integer NOT NULL,
    effective_from date NOT NULL,
    effective_to date,
    CONSTRAINT fk_member_availability_weekly_rules__availability
        FOREIGN KEY (member_id) REFERENCES public.p_member_availability(member_id)
);
CREATE INDEX idx_member_availability_weekly_rules__member
    ON public.p_member_availability_weekly_rules (member_id);

-- 3) p_member_availability_exceptions: 날짜별 예외 (ElementCollection, bag)
CREATE TABLE public.p_member_availability_exceptions (
    member_id bigint NOT NULL,
    exception_date date NOT NULL,
    kind character varying(20) NOT NULL,
    start_slot integer,
    end_slot integer,
    CONSTRAINT fk_member_availability_exceptions__availability
        FOREIGN KEY (member_id) REFERENCES public.p_member_availability(member_id)
);
CREATE INDEX idx_member_availability_exceptions__member
    ON public.p_member_availability_exceptions (member_id);

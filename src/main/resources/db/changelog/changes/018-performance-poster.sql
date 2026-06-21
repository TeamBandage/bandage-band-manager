-- 공연 포스터 테이블 신설 (Performance 1:N PerformancePoster)
CREATE TABLE public.p_performance_poster (
    performance_poster_id uuid NOT NULL,
    performance_id uuid NOT NULL,
    image_key varchar(1024) NOT NULL,
    description text,
    created_at timestamp(6) without time zone NOT NULL,
    created_by bigint,
    last_modified_at timestamp(6) without time zone NOT NULL,
    last_modified_by bigint,
    deleted_at timestamp(6) without time zone,
    deleted_by bigint,
    CONSTRAINT p_performance_poster_pkey PRIMARY KEY (performance_poster_id),
    CONSTRAINT fk_performance_poster__performance
        FOREIGN KEY (performance_id) REFERENCES public.p_performance(performance_id) ON DELETE CASCADE
);

CREATE INDEX idx_performance_poster__performance ON public.p_performance_poster (performance_id);

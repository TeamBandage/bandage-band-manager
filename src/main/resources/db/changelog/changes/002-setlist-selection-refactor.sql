-- =====================================================================
-- 002-setlist-selection-refactor.sql
--
-- BD-43 / BD-59 도메인 리팩토링 반영분.
--
-- 변경 요약:
--   1) Performance ↔ Band/Practice 매핑 테이블 제거
--      - p_performance_band, p_performance_practice 삭제
--      - p_performance_setlist 신설 (Performance ↔ Setlist 매핑)
--
--   2) setlist_meeting 도메인을 track_selection 도메인으로 정리
--      (셋리스트 확정 전 트랙 선정/투표 단계)
--      - p_setlist_meeting               → p_track_selection
--      - p_setlist_meeting_member        → p_track_selection_member
--      - p_setlist_item                  → p_track_selection_item
--      - p_setlist_item_session          → p_track_selection_item_session
--      - p_setlist_item_applicant        → p_track_selection_item_applicant
--      - p_setlist_item_chat_message     → p_track_selection_item_chat_message
--      - p_setlist_item_confirmation     → p_track_selection_item_confirmation
--      - PK/FK 컬럼명: meeting_id → track_selection_id,
--                     meeting_member_id → track_selection_member_id,
--                     setlist_item_id   → track_selection_item_id
--      - p_track_selection 에서 band_id / performance_id / purpose 컬럼 제거
--      - p_track_selection_item 에 is_selected 컬럼 추가 (셋리스트 확정용 플래그)
--      - p_track_selection_band, p_track_selection_member_band 신설
--
--   3) 셋리스트 확정 도메인 신설
--      - p_setlist, p_setlist_band, p_setlist_track,
--        p_setlist_track_session, p_setlist_track_participant 신설
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Performance ↔ Band/Practice 매핑 테이블 제거
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS public.p_performance_band CASCADE;
DROP TABLE IF EXISTS public.p_performance_practice CASCADE;

-- ---------------------------------------------------------------------
-- 2. setlist_meeting 도메인 → track_selection 도메인 rename
--    제약 조건/외래키를 먼저 제거한 뒤 컬럼·테이블을 rename 한다.
-- ---------------------------------------------------------------------

-- 2-1. 외래키 제거 (Hibernate 자동 생성 이름 기준)
ALTER TABLE public.p_setlist_item_confirmation
    DROP CONSTRAINT IF EXISTS fk1jnd47c15svyvj5etnshc54w4;
ALTER TABLE public.p_setlist_item_session
    DROP CONSTRAINT IF EXISTS fk21p0ohliw227nywlmrx9mx90n;
ALTER TABLE public.p_setlist_meeting_member
    DROP CONSTRAINT IF EXISTS fk7lh5uba1i30yorttxhqow5897;
ALTER TABLE public.p_setlist_item_chat_message
    DROP CONSTRAINT IF EXISTS fkeygfgelvoh0by7xjecs5vi8oo;
ALTER TABLE public.p_setlist_item
    DROP CONSTRAINT IF EXISTS fki8gfhyor161vlcwfldxrql7ee;
ALTER TABLE public.p_setlist_item_applicant
    DROP CONSTRAINT IF EXISTS fko1xsrkig3tkkowytdfo1p85ao;

-- 2-2. unique / PK / check 제거
ALTER TABLE public.p_setlist_item_applicant
    DROP CONSTRAINT IF EXISTS uk_setlist_item_applicant;
ALTER TABLE public.p_setlist_item_confirmation
    DROP CONSTRAINT IF EXISTS uk_setlist_item_confirmation;
ALTER TABLE public.p_setlist_meeting_member
    DROP CONSTRAINT IF EXISTS uk_setlist_meeting_member;

ALTER TABLE public.p_setlist_meeting
    DROP CONSTRAINT IF EXISTS p_setlist_meeting_purpose_check;
ALTER TABLE public.p_setlist_meeting
    DROP CONSTRAINT IF EXISTS p_setlist_meeting_pkey;
ALTER TABLE public.p_setlist_meeting_member
    DROP CONSTRAINT IF EXISTS p_setlist_meeting_member_pkey;
ALTER TABLE public.p_setlist_item
    DROP CONSTRAINT IF EXISTS p_setlist_item_pkey;
ALTER TABLE public.p_setlist_item_applicant
    DROP CONSTRAINT IF EXISTS p_setlist_item_applicant_pkey;
ALTER TABLE public.p_setlist_item_confirmation
    DROP CONSTRAINT IF EXISTS p_setlist_item_confirmation_pkey;
ALTER TABLE public.p_setlist_item_chat_message
    DROP CONSTRAINT IF EXISTS p_setlist_item_chat_message_pkey;

-- 2-3. p_setlist_meeting → p_track_selection
ALTER TABLE public.p_setlist_meeting RENAME COLUMN meeting_id TO track_selection_id;
ALTER TABLE public.p_setlist_meeting DROP COLUMN IF EXISTS band_id;
ALTER TABLE public.p_setlist_meeting DROP COLUMN IF EXISTS performance_id;
ALTER TABLE public.p_setlist_meeting DROP COLUMN IF EXISTS purpose;
ALTER TABLE public.p_setlist_meeting RENAME TO p_track_selection;
ALTER TABLE public.p_track_selection
    ADD CONSTRAINT p_track_selection_pkey PRIMARY KEY (track_selection_id);

-- 2-4. p_setlist_meeting_member → p_track_selection_member
ALTER TABLE public.p_setlist_meeting_member RENAME COLUMN meeting_id TO track_selection_id;
ALTER TABLE public.p_setlist_meeting_member RENAME COLUMN meeting_member_id TO track_selection_member_id;
ALTER TABLE public.p_setlist_meeting_member RENAME TO p_track_selection_member;
ALTER TABLE public.p_track_selection_member
    ADD CONSTRAINT p_track_selection_member_pkey PRIMARY KEY (track_selection_member_id);
ALTER TABLE public.p_track_selection_member
    ADD CONSTRAINT uk_track_selection_member UNIQUE (track_selection_id, member_id);
ALTER TABLE public.p_track_selection_member
    ADD CONSTRAINT fk_track_selection_member__selection
    FOREIGN KEY (track_selection_id) REFERENCES public.p_track_selection(track_selection_id);

-- 2-5. p_setlist_item → p_track_selection_item (+ is_selected 컬럼 추가)
ALTER TABLE public.p_setlist_item RENAME COLUMN setlist_item_id TO track_selection_item_id;
ALTER TABLE public.p_setlist_item RENAME COLUMN meeting_id TO track_selection_id;
ALTER TABLE public.p_setlist_item
    ADD COLUMN IF NOT EXISTS is_selected boolean NOT NULL DEFAULT false;
ALTER TABLE public.p_setlist_item RENAME TO p_track_selection_item;
ALTER TABLE public.p_track_selection_item
    ADD CONSTRAINT p_track_selection_item_pkey PRIMARY KEY (track_selection_item_id);
ALTER TABLE public.p_track_selection_item
    ADD CONSTRAINT fk_track_selection_item__selection
    FOREIGN KEY (track_selection_id) REFERENCES public.p_track_selection(track_selection_id);

-- 2-6. p_setlist_item_session → p_track_selection_item_session
ALTER TABLE public.p_setlist_item_session RENAME COLUMN setlist_item_id TO track_selection_item_id;
ALTER TABLE public.p_setlist_item_session RENAME TO p_track_selection_item_session;
ALTER TABLE public.p_track_selection_item_session
    ADD CONSTRAINT fk_track_selection_item_session__item
    FOREIGN KEY (track_selection_item_id) REFERENCES public.p_track_selection_item(track_selection_item_id);

-- 2-7. p_setlist_item_applicant → p_track_selection_item_applicant
ALTER TABLE public.p_setlist_item_applicant RENAME COLUMN setlist_item_id TO track_selection_item_id;
ALTER TABLE public.p_setlist_item_applicant RENAME TO p_track_selection_item_applicant;
ALTER TABLE public.p_track_selection_item_applicant
    ADD CONSTRAINT p_track_selection_item_applicant_pkey PRIMARY KEY (applicant_id);
ALTER TABLE public.p_track_selection_item_applicant
    ADD CONSTRAINT uk_track_selection_item_applicant
    UNIQUE (track_selection_item_id, session_id, member_id);
ALTER TABLE public.p_track_selection_item_applicant
    ADD CONSTRAINT fk_track_selection_item_applicant__item
    FOREIGN KEY (track_selection_item_id) REFERENCES public.p_track_selection_item(track_selection_item_id);

-- 2-8. p_setlist_item_confirmation → p_track_selection_item_confirmation
ALTER TABLE public.p_setlist_item_confirmation RENAME COLUMN setlist_item_id TO track_selection_item_id;
ALTER TABLE public.p_setlist_item_confirmation RENAME TO p_track_selection_item_confirmation;
ALTER TABLE public.p_track_selection_item_confirmation
    ADD CONSTRAINT p_track_selection_item_confirmation_pkey PRIMARY KEY (confirmation_id);
ALTER TABLE public.p_track_selection_item_confirmation
    ADD CONSTRAINT uk_track_selection_item_confirmation
    UNIQUE (track_selection_item_id, session_id, member_id);
ALTER TABLE public.p_track_selection_item_confirmation
    ADD CONSTRAINT fk_track_selection_item_confirmation__item
    FOREIGN KEY (track_selection_item_id) REFERENCES public.p_track_selection_item(track_selection_item_id);

-- 2-9. p_setlist_item_chat_message → p_track_selection_item_chat_message
ALTER TABLE public.p_setlist_item_chat_message RENAME COLUMN setlist_item_id TO track_selection_item_id;
ALTER TABLE public.p_setlist_item_chat_message RENAME TO p_track_selection_item_chat_message;
ALTER TABLE public.p_track_selection_item_chat_message
    ADD CONSTRAINT p_track_selection_item_chat_message_pkey PRIMARY KEY (message_id);
ALTER TABLE public.p_track_selection_item_chat_message
    ADD CONSTRAINT fk_track_selection_item_chat_message__item
    FOREIGN KEY (track_selection_item_id) REFERENCES public.p_track_selection_item(track_selection_item_id);

-- ---------------------------------------------------------------------
-- 3. track_selection 보조 테이블 신설
-- ---------------------------------------------------------------------
CREATE TABLE public.p_track_selection_band (
    created_at timestamp(6) without time zone NOT NULL,
    created_by bigint,
    deleted_at timestamp(6) without time zone,
    deleted_by bigint,
    last_modified_at timestamp(6) without time zone NOT NULL,
    last_modified_by bigint,
    track_selection_band_id uuid NOT NULL,
    track_selection_id uuid NOT NULL,
    band_id uuid NOT NULL,
    CONSTRAINT p_track_selection_band_pkey PRIMARY KEY (track_selection_band_id),
    CONSTRAINT uk_track_selection_band UNIQUE (track_selection_id, band_id),
    CONSTRAINT fk_track_selection_band__selection
        FOREIGN KEY (track_selection_id) REFERENCES public.p_track_selection(track_selection_id)
);

CREATE TABLE public.p_track_selection_member_band (
    track_selection_member_id uuid NOT NULL,
    band_id uuid NOT NULL,
    CONSTRAINT uk_track_selection_member_band UNIQUE (track_selection_member_id, band_id),
    CONSTRAINT fk_track_selection_member_band__member
        FOREIGN KEY (track_selection_member_id) REFERENCES public.p_track_selection_member(track_selection_member_id)
);

-- ---------------------------------------------------------------------
-- 4. 셋리스트 확정 도메인 신설
-- ---------------------------------------------------------------------
CREATE TABLE public.p_setlist (
    created_at timestamp(6) without time zone NOT NULL,
    created_by bigint,
    deleted_at timestamp(6) without time zone,
    deleted_by bigint,
    last_modified_at timestamp(6) without time zone NOT NULL,
    last_modified_by bigint,
    setlist_id uuid NOT NULL,
    track_selection_id uuid NOT NULL,
    manager_id bigint NOT NULL,
    title character varying(255) NOT NULL,
    CONSTRAINT p_setlist_pkey PRIMARY KEY (setlist_id)
);

CREATE TABLE public.p_setlist_band (
    created_at timestamp(6) without time zone NOT NULL,
    created_by bigint,
    deleted_at timestamp(6) without time zone,
    deleted_by bigint,
    last_modified_at timestamp(6) without time zone NOT NULL,
    last_modified_by bigint,
    setlist_band_id uuid NOT NULL,
    setlist_id uuid NOT NULL,
    band_id uuid NOT NULL,
    CONSTRAINT p_setlist_band_pkey PRIMARY KEY (setlist_band_id)
);

CREATE TABLE public.p_setlist_track (
    created_at timestamp(6) without time zone NOT NULL,
    created_by bigint,
    deleted_at timestamp(6) without time zone,
    deleted_by bigint,
    last_modified_at timestamp(6) without time zone NOT NULL,
    last_modified_by bigint,
    setlist_track_id uuid NOT NULL,
    setlist_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    artist character varying(255) NOT NULL,
    album character varying(255),
    duration character varying(255),
    note character varying(1000),
    practice_song_id uuid,
    CONSTRAINT p_setlist_track_pkey PRIMARY KEY (setlist_track_id),
    CONSTRAINT fk_setlist_track__setlist
        FOREIGN KEY (setlist_id) REFERENCES public.p_setlist(setlist_id)
);

CREATE TABLE public.p_setlist_track_session (
    setlist_track_id uuid NOT NULL,
    session_id character varying(255) NOT NULL,
    label character varying(255) NOT NULL,
    session_short character varying(255) NOT NULL,
    session_need integer NOT NULL,
    session_custom boolean NOT NULL,
    CONSTRAINT fk_setlist_track_session__track
        FOREIGN KEY (setlist_track_id) REFERENCES public.p_setlist_track(setlist_track_id)
);

CREATE TABLE public.p_setlist_track_participant (
    created_at timestamp(6) without time zone NOT NULL,
    created_by bigint,
    deleted_at timestamp(6) without time zone,
    deleted_by bigint,
    last_modified_at timestamp(6) without time zone NOT NULL,
    last_modified_by bigint,
    participant_id uuid NOT NULL,
    setlist_track_id uuid NOT NULL,
    session_id character varying(255) NOT NULL,
    member_id bigint NOT NULL,
    CONSTRAINT p_setlist_track_participant_pkey PRIMARY KEY (participant_id),
    CONSTRAINT uk_setlist_track_participant
        UNIQUE (setlist_track_id, session_id, member_id),
    CONSTRAINT fk_setlist_track_participant__track
        FOREIGN KEY (setlist_track_id) REFERENCES public.p_setlist_track(setlist_track_id)
);

-- ---------------------------------------------------------------------
-- 5. p_performance_setlist 신설 (Performance ↔ Setlist 매핑)
-- ---------------------------------------------------------------------
CREATE TABLE public.p_performance_setlist (
    created_at timestamp(6) without time zone NOT NULL,
    created_by bigint,
    deleted_at timestamp(6) without time zone,
    deleted_by bigint,
    last_modified_at timestamp(6) without time zone NOT NULL,
    last_modified_by bigint,
    performance_setlist_id uuid NOT NULL,
    performance_id uuid NOT NULL,
    setlist_id uuid NOT NULL,
    CONSTRAINT p_performance_setlist_pkey PRIMARY KEY (performance_setlist_id),
    CONSTRAINT uk_performance_setlist UNIQUE (performance_id, setlist_id),
    CONSTRAINT fk_performance_setlist__performance
        FOREIGN KEY (performance_id) REFERENCES public.p_performance(performance_id)
);

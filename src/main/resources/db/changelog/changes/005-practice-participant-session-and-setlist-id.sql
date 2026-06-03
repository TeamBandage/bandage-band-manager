-- BD-70 P1-c: 세션/참여자 모델 정합화 + setlistId 추적 필드
-- 운영 데이터 없음 가정.

-- 1) p_practice.setlist_id 추가 (Setlist 경유 생성 추적용, nullable)
ALTER TABLE public.p_practice ADD COLUMN setlist_id uuid;

-- 2) p_practice_participant: session_id 추가 + (practice_id, session_id, member_id) UK
ALTER TABLE public.p_practice_participant ADD COLUMN session_id character varying(255) NOT NULL DEFAULT '';
ALTER TABLE public.p_practice_participant ALTER COLUMN session_id DROP DEFAULT;
ALTER TABLE public.p_practice_participant
    ADD CONSTRAINT uk_practice_participant UNIQUE (practice_id, session_id, member_id);

-- 3) PracticeSession 엔티티 테이블 제거 후, SessionDef ElementCollection 테이블로 재생성
DROP TABLE IF EXISTS public.p_practice_session;
CREATE TABLE public.p_practice_session (
    practice_id uuid NOT NULL,
    session_id character varying(255) NOT NULL,
    label character varying(255) NOT NULL,
    session_short character varying(255) NOT NULL,
    session_need integer NOT NULL,
    session_custom boolean NOT NULL,
    CONSTRAINT fk_practice_session__practice
        FOREIGN KEY (practice_id) REFERENCES public.p_practice(practice_id)
);

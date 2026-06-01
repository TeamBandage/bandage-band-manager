-- BD-70 P2: Practice → Jam 도메인 리네이밍 (테이블/컬럼/제약 일괄 rename)
-- Postgres 의 RENAME TO 는 의존 FK 참조를 자동 갱신하므로 데이터 보존됨.

-- 1) 테이블 rename
ALTER TABLE public.p_practice RENAME TO p_jam;
ALTER TABLE public.p_practice_participant RENAME TO p_jam_participant;
ALTER TABLE public.p_practice_session RENAME TO p_jam_session;

-- 2) 컬럼 rename
ALTER TABLE public.p_jam RENAME COLUMN practice_id TO jam_id;
ALTER TABLE public.p_jam_participant RENAME COLUMN practice_participant_id TO jam_participant_id;
ALTER TABLE public.p_jam_participant RENAME COLUMN practice_id TO jam_id;
ALTER TABLE public.p_jam_session RENAME COLUMN practice_id TO jam_id;

-- 3) 제약 rename (가독성 정리. FK 컬럼 참조는 컬럼 rename 으로 자동 갱신됨)
ALTER TABLE public.p_jam RENAME CONSTRAINT p_practice_pkey TO p_jam_pkey;
ALTER TABLE public.p_jam_participant RENAME CONSTRAINT p_practice_participant_pkey TO p_jam_participant_pkey;
ALTER TABLE public.p_jam_participant RENAME CONSTRAINT uk_practice_participant TO uk_jam_participant;
ALTER TABLE public.p_jam_participant RENAME CONSTRAINT fk8jbcy18m52b8gb459hdk3rwyb TO fk_jam_participant__jam;
ALTER TABLE public.p_jam_session RENAME CONSTRAINT fk_practice_session__practice TO fk_jam_session__jam;

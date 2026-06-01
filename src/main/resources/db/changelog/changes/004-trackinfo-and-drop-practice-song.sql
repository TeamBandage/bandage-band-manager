-- BD-70 P1-a/b: TrackInfo 임베디드 도입 및 PracticeSong 제거
-- 운영 데이터 없음 가정. duration 은 varchar(mm:ss) → integer(초) 로 단순 변환.

-- 1) p_practice: song_id FK/UNIQUE 제거 및 컬럼 제거, track_* + note 컬럼 추가
ALTER TABLE public.p_practice DROP CONSTRAINT IF EXISTS fkoierh8dymh6ktaya3f2hxxk90;
ALTER TABLE public.p_practice DROP CONSTRAINT IF EXISTS p_practice_song_id_key;
ALTER TABLE public.p_practice DROP COLUMN IF EXISTS song_id;

ALTER TABLE public.p_practice ADD COLUMN track_title character varying(255) NOT NULL DEFAULT '';
ALTER TABLE public.p_practice ALTER COLUMN track_title DROP DEFAULT;
ALTER TABLE public.p_practice ADD COLUMN track_artist character varying(255) NOT NULL DEFAULT '';
ALTER TABLE public.p_practice ALTER COLUMN track_artist DROP DEFAULT;
ALTER TABLE public.p_practice ADD COLUMN track_album character varying(255);
ALTER TABLE public.p_practice ADD COLUMN track_duration integer;
ALTER TABLE public.p_practice ADD COLUMN track_reference character varying(255);
ALTER TABLE public.p_practice ADD COLUMN note character varying(1000);

-- 2) p_practice_song 테이블 제거
DROP TABLE IF EXISTS public.p_practice_song;

-- 3) p_setlist_track: practice_song_id 제거, duration varchar→int, reference 추가
ALTER TABLE public.p_setlist_track DROP COLUMN IF EXISTS practice_song_id;
ALTER TABLE public.p_setlist_track ALTER COLUMN duration TYPE integer USING NULLIF(duration, '')::integer;
ALTER TABLE public.p_setlist_track ADD COLUMN reference character varying(255);

-- 4) p_track_selection_item: practice_song_id 제거, duration varchar→int, reference 추가
ALTER TABLE public.p_track_selection_item DROP COLUMN IF EXISTS practice_song_id;
ALTER TABLE public.p_track_selection_item ALTER COLUMN duration TYPE integer USING NULLIF(duration, '')::integer;
ALTER TABLE public.p_track_selection_item ADD COLUMN reference character varying(255);

-- BD-205: JamParticipant(합주 소속)와 JamParticipantSession(세션 배정) 분리
--         기존 p_jam_participant(jam_id, session_id, member_id)는 "소속"과 "세션 배정" 두 관계를
--         한 테이블에 담아, 세션 삭제가 참여자 삭제/중복을 유발하는 문제가 있었다(BD-205).
--         이제 p_jam_participant는 순수 소속(jam_id, member_id)만 표현하고,
--         세션 배정은 p_jam_participant_session(jam_participant_id, session_id)으로 분리한다.

-- 1) 배정 테이블 신설
CREATE TABLE public.p_jam_participant_session (
    jam_participant_session_id uuid PRIMARY KEY,
    jam_participant_id uuid NOT NULL,
    session_id varchar(255) NOT NULL,
    created_at timestamp NOT NULL,
    created_by bigint,
    last_modified_at timestamp NOT NULL,
    last_modified_by bigint,
    CONSTRAINT fk_jam_participant_session__jam_participant
        FOREIGN KEY (jam_participant_id) REFERENCES public.p_jam_participant (jam_participant_id),
    CONSTRAINT uk_jam_participant_session UNIQUE (jam_participant_id, session_id)
);

-- 2) (jam_id, member_id) 별 대표 소속 행 선정
--    기존에는 멤버가 세션 N개에 배정되면 p_jam_participant 행도 N개였다(jam_id, session_id, member_id UK).
--    이행 후에는 (jam_id, member_id) 당 1행만 남아야 하므로, UUIDv7(시간순 정렬)의 최솟값 = 최초 생성 행을 대표로 삼는다.
CREATE TEMP TABLE tmp_jam_participant_representative AS
SELECT jam_id, member_id, MIN(jam_participant_id::text)::uuid AS representative_id
FROM public.p_jam_participant
WHERE deleted_at IS NULL
GROUP BY jam_id, member_id;

-- 3) 기존 세션 배정 데이터 이행 (session_id IS NOT NULL 인 행만) — 대표 행 아래로 재배정
INSERT INTO public.p_jam_participant_session (
    jam_participant_session_id, jam_participant_id, session_id,
    created_at, created_by, last_modified_at, last_modified_by
)
SELECT gen_random_uuid(), r.representative_id, p.session_id,
       p.created_at, p.created_by, p.last_modified_at, p.last_modified_by
FROM public.p_jam_participant p
JOIN tmp_jam_participant_representative r
  ON r.jam_id = p.jam_id AND r.member_id = p.member_id
WHERE p.session_id IS NOT NULL AND p.deleted_at IS NULL;

-- 4) 대표가 아닌 중복 소속 행 삭제 (세션 배정은 이미 대표 행으로 이행 완료)
DELETE FROM public.p_jam_participant p
WHERE p.deleted_at IS NULL
  AND p.jam_participant_id NOT IN (SELECT representative_id FROM tmp_jam_participant_representative);

DROP TABLE tmp_jam_participant_representative;

-- 5) p_jam_participant 정리: session_id 컬럼/제약 제거, (jam_id, member_id) 단일 소속 제약으로 전환
ALTER TABLE public.p_jam_participant DROP CONSTRAINT uk_jam_participant;
ALTER TABLE public.p_jam_participant DROP COLUMN session_id;
CREATE UNIQUE INDEX uk_jam_participant_active
    ON public.p_jam_participant (jam_id, member_id) WHERE deleted_at IS NULL;

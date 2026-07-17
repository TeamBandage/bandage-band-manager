-- BD-205 후속 수정: p_jam_participant_session 은 JamParticipantSession(BaseEntity 상속)의 테이블이지만
--                   022 마이그레이션에서 soft delete 컬럼(deleted_at, deleted_by)이 누락되어
--                   Hibernate 스키마 검증(ddl-auto=validate) 실패를 유발했다. 컬럼을 보강한다.

ALTER TABLE public.p_jam_participant_session ADD COLUMN deleted_at timestamp(6) without time zone;
ALTER TABLE public.p_jam_participant_session ADD COLUMN deleted_by bigint;

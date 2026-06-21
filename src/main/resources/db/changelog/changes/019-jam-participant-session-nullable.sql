-- BD-154: 합주 생성자를 세션 미배정(소속) 참여자로 자동 등록하기 위해 session_id NOT NULL 해제.
--         생성자는 createJam 시 session_id=NULL 인 "소속" 레코드로 등록되어 "내 합주 목록"에 노출되고,
--         이후 세션 변경 API로 본인 세션을 지정(NULL→토큰)한다.
-- 주의: uk_jam_participant(jam_id, session_id, member_id) 는 session_id=NULL 행을 중복 허용(Postgres NULL distinct).
--       소속 레코드는 createJam 에서 멤버당 1회만 생성되므로 앱 레벨에서 중복이 발생하지 않는다.
ALTER TABLE public.p_jam_participant ALTER COLUMN session_id DROP NOT NULL;

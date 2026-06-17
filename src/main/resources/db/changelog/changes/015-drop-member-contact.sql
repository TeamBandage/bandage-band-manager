-- BD-84: Member contact 컬럼 제거 (개인정보 노출 포인트 제거, 미사용 필드)

ALTER TABLE public.p_member
    DROP COLUMN contact;

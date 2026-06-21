-- BD-14: soft delete 리소스의 plain UNIQUE → partial unique index 전환
--        활성(deleted_at IS NULL) 행만 유일성 보장 → soft delete 후 동일 email/name 재등록 허용.
--        plain UNIQUE는 삭제 행까지 점유하여 재가입을 막고, existsByEmail(@SQLRestriction, 활성만 조회)과
--        DB 제약이 어긋나 재가입 시 raw DataIntegrityViolationException(500)이 새던 문제도 함께 해소.
--
--        주의: 이후 changeset/코드에서 ON CONFLICT (email)/(name) 을 쓰려면 반드시
--              'WHERE deleted_at IS NULL' predicate 를 함께 명시해야 partial index 가 arbiter 로 선택됨.

-- 1) p_member.email
ALTER TABLE public.p_member DROP CONSTRAINT p_member_email_key;
CREATE UNIQUE INDEX uk_member_email_active
    ON public.p_member (email) WHERE deleted_at IS NULL;

-- 2) p_member_auth.email
ALTER TABLE public.p_member_auth DROP CONSTRAINT p_member_auth_email_key;
CREATE UNIQUE INDEX uk_member_auth_email_active
    ON public.p_member_auth (email) WHERE deleted_at IS NULL;

-- 3) p_band.name
ALTER TABLE public.p_band DROP CONSTRAINT p_band_name_key;
CREATE UNIQUE INDEX uk_band_name_active
    ON public.p_band (name) WHERE deleted_at IS NULL;

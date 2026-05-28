-- =====================================================================
-- 003-seed-developer-account.sql
--
-- 개발용 기본 DEVELOPER 계정 시드
--   email    : developer@bandage.com
--   password : 11111111  (BCrypt cost=10 해시 저장)
--   role     : DEVELOPER
--
-- Spring Security BCryptPasswordEncoder 는 $2a / $2b / $2y prefix 를
-- 모두 인식하므로 htpasswd 결과($2y) 를 그대로 사용한다.
--
-- 멱등성을 위해 ON CONFLICT 처리. 이미 동일 email/PK 가 존재하면 무시.
-- =====================================================================

INSERT INTO public.p_member (
    created_at,
    last_modified_at,
    email,
    name,
    contact,
    profile_img
)
VALUES (
    NOW(),
    NOW(),
    'developer@bandage.com',
    'Developer',
    NULL,
    NULL
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO public.p_member_auth (
    created_at,
    last_modified_at,
    member_id,
    email,
    password,
    provider,
    role
)
SELECT
    NOW(),
    NOW(),
    m.member_id,
    m.email,
    '$2y$10$jg.hk6myapBDQPsIMGFPOui37Nv3TqDdsIkeztbqAFZYLfT5CrarO',
    'LOCAL',
    'DEVELOPER'
FROM public.p_member m
WHERE m.email = 'developer@bandage.com'
ON CONFLICT (member_id) DO NOTHING;

-- BD-210: 밴드 가입 신청 재지원 시 과거/최신 이력 구분
--         기존에는 재지원마다 새 row 만 추가하고 과거 이력을 구분할 방법이 없어,
--         한 회원이 특정 밴드에 여러 번 지원한 경우 거절/승인 등 여러 상태가 동시에 조회되는 문제가 있었다.
--         (band_id, member_id) 당 가장 최근 지원 건에만 is_latest = true 를 표시해 최신 상태만 구분 조회할 수 있게 한다.

ALTER TABLE public.p_band_application ADD COLUMN is_latest boolean NOT NULL DEFAULT true;

-- 기존 데이터 백필: (band_id, member_id) 당 가장 최근(created_at, 동률 시 id) 1건만 true, 나머지는 false
WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY band_id, member_id
               ORDER BY created_at DESC, id DESC
           ) AS rn
    FROM public.p_band_application
    WHERE deleted_at IS NULL
)
UPDATE public.p_band_application a
SET is_latest = false
FROM ranked
WHERE ranked.id = a.id AND ranked.rn > 1;

CREATE UNIQUE INDEX uk_band_application_latest
    ON public.p_band_application (band_id, member_id) WHERE is_latest AND deleted_at IS NULL;

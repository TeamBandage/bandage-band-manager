-- BD-256: TrackSelection 에서 합주 가능 기간(practiceWindow) 제거.
--         합주 기간 처리는 Schedule 도메인으로 이전한다.
ALTER TABLE public.p_track_selection DROP COLUMN practice_window_from;
ALTER TABLE public.p_track_selection DROP COLUMN practice_window_to;

-- BD-218: 선곡 회의 떠나기 시 제안자 연결을 해제할 수 있도록 nullable 전환.
--         제안자가 떠나도 아이템 자체는 유지하고 proposer_id 만 NULL 로 만든다.
ALTER TABLE public.p_track_selection_item ALTER COLUMN proposer_id DROP NOT NULL;

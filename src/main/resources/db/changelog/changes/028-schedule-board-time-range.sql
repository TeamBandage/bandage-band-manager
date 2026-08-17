-- BD-272: 미사용 보드 제약(ScheduleBoardConstraints) 제거 및 배치 가능 시간대 필드 정리
--         working_hours_* 는 시(hour)가 아니라 하루 안의 시간대(슬롯 인덱스)이므로
--         이름을 board_time_range_* 로 바로잡는다. 값은 보존된다.
--         exclude_late_night / max_consecutive_minutes 는 "심야" 정의도, 슬롯 변환도,
--         읽는 로직도 존재한 적이 없어 제거한다.

ALTER TABLE public.p_schedule_board RENAME COLUMN working_hours_start TO board_time_range_from;
ALTER TABLE public.p_schedule_board RENAME COLUMN working_hours_end TO board_time_range_to;

ALTER TABLE public.p_schedule_board DROP COLUMN exclude_late_night;
ALTER TABLE public.p_schedule_board DROP COLUMN max_consecutive_minutes;

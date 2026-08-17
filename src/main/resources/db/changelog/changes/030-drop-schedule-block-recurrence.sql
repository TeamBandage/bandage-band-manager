-- BD-272: ScheduleBlock 의 반복 규칙(RecurrenceRule) 제거
--         반복 전개를 담당하기로 했던 ScheduleConfirmFacade.expandRecurrence 는 끝내 구현되지 않았고,
--         자동배치는 반복 블록 1개를 두는 대신 회차 수만큼 블록을 실제로 생성하는 방식을 택했다.
--         따라서 이 4개 컬럼은 저장만 되고 읽는 로직이 없는 상태였다.

ALTER TABLE public.p_schedule_block DROP COLUMN recurrence_freq;
ALTER TABLE public.p_schedule_block DROP COLUMN recurrence_interval;
ALTER TABLE public.p_schedule_block DROP COLUMN recurrence_until;
ALTER TABLE public.p_schedule_block DROP COLUMN recurrence_count;
